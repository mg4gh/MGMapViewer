package mg.mgmap.activity.mgmap.features.shareloc;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.UUID;

import mg.mgmap.R;

public class Registration {
    private static final String TAG = "Registration";
    private final AppCompatActivity activity;
    private MqttBase mqttClient;
    private boolean isAwaitingConfirmation = false;
    private KeyPair currentKeyPair;
    private String email;

    public Registration(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void show(File baseDir) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_register, null);

        TextView tvError = dialogView.findViewById(R.id.tvError);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etConfirmationCode = dialogView.findViewById(R.id.etConfirmationCode);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        ImageButton btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        try (InputStream caCrt = activity.getAssets().open("ca.crt");
             InputStream clientCrt = activity.getAssets().open("client.crt");
             InputStream clientKey = activity.getAssets().open("client.key")) {

            mqttClient = new MqttBase(caCrt, clientCrt, clientKey) {
                @Override
                public void messageReceived(String topic, String message) {
                    Log.i(TAG, "Message arrived. Topic: " + topic + " Payload: " + message);
                    if (topic.equals("/server/" + clientId + "/register_confirm")) {
                        handleRegisterConfirm(message, etConfirmationCode, progressBar, btnSubmit, dialogView);
                    } else if (topic.equals("/server/" + clientId + "/register_response")) {
                        handleRegisterResponse(message, baseDir, dialog);
                    }
                }

                @Override
                protected void registerThrowable(Throwable throwable) {
                    Log.e(TAG, "MQTT error", throwable);
                    activity.runOnUiThread(() -> {
                        tvError.setText(throwable.getMessage());
                        tvError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        // Trigger watcher to update button state
                        etEmail.setText(etEmail.getText()); 
                    });
                }
            };

            mqttClient.registerTopic("/server/" + mqttClient.clientId + "/#");

        } catch (Exception e) {
            if (mqttClient != null) {
                mqttClient.registerThrowable(e);
            } else {
                Log.e(TAG, "MQTT initialization error", e);
                Toast.makeText(activity, "Failed to connect to server: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        isAwaitingConfirmation = false;

        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isInputValid;
                if (!isAwaitingConfirmation) {
                    isInputValid = Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches();
                } else {
                    isInputValid = isValidUuid(etConfirmationCode.getText().toString());
                }

                boolean hasError = tvError.getVisibility() == View.VISIBLE;
                boolean canSubmit = isInputValid && mqttClient != null && !hasError;
                
                btnSubmit.setEnabled(canSubmit);
                if (canSubmit) {
                    btnSubmit.setColorFilter(Color.BLUE);
                    btnSubmit.setAlpha(1.0f);
                } else {
                    btnSubmit.clearColorFilter();
                    btnSubmit.setAlpha(0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etEmail.addTextChangedListener(commonWatcher);
        etConfirmationCode.addTextChangedListener(commonWatcher);

        // Initial check to set button state correctly
        commonWatcher.onTextChanged("", 0, 0, 0);

        btnCancel.setOnClickListener(v -> {
            stopMqtt();
            dialog.dismiss();
        });

        @SuppressWarnings("ResultOfMethodCallIgnored")
        View.OnClickListener submitListener = v -> {
            tvError.setVisibility(View.GONE);
            
            if (!isAwaitingConfirmation) {
                // Phase 1: Send registration request
                this.email = etEmail.getText().toString();

                etEmail.setEnabled(false);
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.5f);
                progressBar.setVisibility(View.VISIBLE);

                try {
                    mqttClient.sendMessage("/client/server/register_init", mqttClient.clientId + ":" + email);
                } catch (Exception e) {
                    mqttClient.registerThrowable(e);
                }
            } else {
                // Phase 2: Send confirmation request
                String code = etConfirmationCode.getText().toString();

                etConfirmationCode.setEnabled(false);
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.5f);
                progressBar.setVisibility(View.VISIBLE);

                try {
                    // Generate KeyPair
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(2048, new SecureRandom());
                    currentKeyPair = keyGen.generateKeyPair();

                    // Save locally generated private key to baseDir/certs/my.key
                    File certsDir = new File(baseDir, "certs");
                    if (!certsDir.exists()) {
                        certsDir.mkdirs();
                    }
                    File myKeyFile = new File(certsDir, "my.key");
                    try (StringWriter swKey = new StringWriter();
                         JcaPEMWriter pemWriter = new JcaPEMWriter(swKey)) {
                        pemWriter.writeObject(currentKeyPair.getPrivate());
                        pemWriter.flush();
                        try (FileWriter fw = new FileWriter(myKeyFile)) {
                            fw.write(swKey.toString());
                        }
                    }

                    // Generate CSR
                    X500Name subject = new X500Name("CN=" + etEmail.getText().toString());
                    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(subject, currentKeyPair.getPublic());
                    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(currentKeyPair.getPrivate());
                    PKCS10CertificationRequest csr = csrBuilder.build(signer);

                    StringWriter sw = new StringWriter();
                    try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
                        pemWriter.writeObject(csr);
                    }
                    String pemCsr = sw.toString();

                    String hashedCode = CryptoUtils.sha256(code);
                    String payload = mqttClient.clientId + ":" + hashedCode + ":" + pemCsr;
                    mqttClient.sendMessage("/client/server/register_request", payload);
                } catch (Exception e) {
                    mqttClient.registerThrowable(e);
                }
            }
        };
        btnSubmit.setOnClickListener(submitListener);

        dialog.show();
    }

    private void handleRegisterConfirm(String message, EditText etConfirmationCode, ProgressBar progressBar, ImageButton btnSubmit, View dialogView) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            boolean success = Boolean.parseBoolean(parts[0]);
            String info = parts[1];

            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    isAwaitingConfirmation = true;
                    etConfirmationCode.setVisibility(View.VISIBLE);
                    btnSubmit.setEnabled(false);
                    btnSubmit.setAlpha(0.5f);
                    btnSubmit.clearColorFilter();
                    Toast.makeText(activity, "Please enter the code from your email", Toast.LENGTH_LONG).show();
                } else {
                    TextView tvError = dialogView.findViewById(R.id.tvError);
                    tvError.setText("Server error: " + info);
                    tvError.setVisibility(View.VISIBLE);
                    dialogView.findViewById(R.id.etEmail).setEnabled(true);
                    btnSubmit.setEnabled(false);
                    btnSubmit.setAlpha(0.5f);
                }
            });
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleRegisterResponse(String message, File baseDir, AlertDialog dialog) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            String extra1 = parts[0];
            String extra2 = parts[1];

            boolean success = extra1.equals("success");
            if (success) {
                try {
                    File certsDir = new File(baseDir, "certs");
                    if (!certsDir.exists()) {
                        certsDir.mkdirs();
                    }
                    File userCrt = new File(certsDir, "my.crt");
                    try (FileWriter writer = new FileWriter(userCrt)) {
                        writer.write(extra2);
                    }

                    // Start Verification with newly received certificate and local key
                    try(InputStream caCrt = activity.getAssets().open("ca.crt");
                        InputStream myCrt = new FileInputStream(new File(certsDir, "my.crt"));
                        InputStream myKey = new FileInputStream(new File(certsDir, "my.key")) ){

                        MqttBase verifyMqtt = new MqttBase(caCrt, myCrt, myKey) {
                            @Override
                            public void messageReceived(String topic, String message) {
                                Log.i(TAG, "Message arrived. Topic: " + topic + " Payload: " + message);
                                if (topic.equals("/server/" + email + "/verify_confirm")) {
                                    activity.runOnUiThread(() -> {
                                        Toast.makeText(activity, "Registration successful finished.", Toast.LENGTH_SHORT).show();
                                        stopMqtt();
                                        dialog.dismiss();
                                    });

                                }
                            }
                        };
                        verifyMqtt.registerTopic("/server/" + email + "/verify_confirm");
                        verifyMqtt.sendMessage("/" + email + "/server/register_verify", "register_verify");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing register response", e);
                    success = false;
                    extra2 = e.getMessage();
                }
            }
            String finalExtra2 = extra2;
            if (!success){
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Registration failed : "+ finalExtra2, Toast.LENGTH_LONG).show();
                    stopMqtt();
                    dialog.dismiss();
                });
            }
        }
    }

    private boolean isValidUuid(String target) {
        try {
            UUID.fromString(target);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void stopMqtt() {
        if (mqttClient != null && mqttClient.client != null) {
            new Thread(() -> {
                try {
                    mqttClient.client.disconnect(0l);
                    mqttClient.client.close();
                } catch (MqttException e) {
                    Log.e(TAG, "Error closing MQTT", e);
                }
            }).start();
        }
    }
}
