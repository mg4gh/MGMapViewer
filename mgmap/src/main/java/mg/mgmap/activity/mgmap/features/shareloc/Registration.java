package mg.mgmap.activity.mgmap.features.shareloc;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import java.lang.invoke.MethodHandles;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import mg.mgmap.R;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;

public class Registration {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final AppCompatActivity activity;
    SharePerson me;

    private MqttBase mqttClient;
    private boolean isAwaitingConfirmation = false;
    private KeyPair currentKeyPair;
    private String email;

    DialogView dialogViewChild;
    View registrationDialogView;
    TextView tvError;
    EditText etEmail;
    EditText etConfirmationCode;
    ProgressBar progressBar;
    private final Pref<Boolean> recalcEnabledTrigger = new Pref<>(true);

    @SuppressLint("InflateParams")
    public Registration(AppCompatActivity activity, SharePerson me) {
        this.activity = activity;
        this.me = me;
        dialogViewChild = new DialogView(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        registrationDialogView = inflater.inflate(R.layout.dialog_register, null);
        tvError = registrationDialogView.findViewById(R.id.tvError);
        etEmail = registrationDialogView.findViewById(R.id.etEmail);
        etConfirmationCode = registrationDialogView.findViewById(R.id.etConfirmationCode);
        progressBar = registrationDialogView.findViewById(R.id.progressBar);
    }

    @SuppressLint("SetTextI18n")
    public void show(File baseDir) {
        Pref<Boolean> enablePref = new Pref<>(false);
        enablePref.addObserver(pce->recalcEnabledTrigger.toggle());
        recalcEnabledTrigger.addObserver(pce->{
            boolean isInputValid = enablePref.getValue();
            boolean hasError = tvError.getVisibility() == View.VISIBLE;
            boolean canSubmit = isInputValid && (mqttClient != null) && !hasError;
            dialogViewChild.setEnablePositive(canSubmit);
        });

        try (InputStream caCrt = activity.getAssets().open("ca.crt");
             InputStream clientCrt = activity.getAssets().open("client.crt");
             InputStream clientKey = activity.getAssets().open("client.key")) {

            mqttClient = new MqttBase("ClientInit",caCrt, clientCrt, clientKey) {
                @Override
                public void messageReceived(String topic, String message) {
                    super.messageReceived(topic, message);
                    if (topic.equals("/server/" + getClientId() + "/register_confirm")) {
                        handleRegisterConfirm(message);
                    } else if (topic.equals("/server/" + getClientId() + "/register_response")) {
                        handleRegisterResponse(message, baseDir);
                    }
                }

                @Override
                protected void registerThrowable(Throwable throwable) {
                    mgLog.e("MQTT error", throwable);
                    activity.runOnUiThread(() -> showError("MQTT error"));
                }

                @Override
                protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                    if (!reconnect){
                        mqttClient.registerTopic("/server/" + mqttClient.getClientId() + "/#");
                    }
                }
            };

        } catch (Exception e) {
            if (mqttClient != null) {
                mqttClient.registerThrowable(e);
            } else {
                mgLog.e("MQTT initialization error", e);
                showError("MQTT initialization error "+e.getMessage());
            }
        }

        isAwaitingConfirmation = false;

        TextWatcherEmail twe = new TextWatcherEmail(enablePref);
        etEmail.addTextChangedListener(twe);
        etEmail.setText(me.email);
        etEmail.setSelectAllOnFocus(true);
        etEmail.requestFocus();

        TextWatcherConfirmation twc = new TextWatcherConfirmation(enablePref);
        etConfirmationCode.addTextChangedListener(twc);

        @SuppressWarnings("ResultOfMethodCallIgnored")
        Observer submitObserver = propertyChangeEvent -> {
            tvError.setVisibility(View.GONE);

            if (!isAwaitingConfirmation) {
                // Phase 1: Send registration request
                Registration.this.email = etEmail.getText().toString();
                etEmail.setEnabled(false);
                dialogViewChild.setEnablePositive(false);
                progressBar.setVisibility(View.VISIBLE);
                try {
                    mqttClient.sendMessage("/client/server/register_init", mqttClient.getClientId() + ":" + email);
                } catch (Exception e) {
                    mqttClient.registerThrowable(e);
                }
            } else {
                // Phase 2: Send confirmation request
                String code = etConfirmationCode.getText().toString();
                etConfirmationCode.setEnabled(false);
                dialogViewChild.setEnablePositive(false);
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
                    String payload = mqttClient.getClientId() + ":" + hashedCode + ":" + pemCsr;
                    mqttClient.sendMessage("/client/server/register_request", payload);
                } catch (Exception e) {
                    mqttClient.registerThrowable(e);
                }
            }
        };


        dialogViewChild.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Register at MGMapServer")
                .setContentView(registrationDialogView)
                .setPositive("Submit",submitObserver, false)
                .setNegative("Cancel",pce->{
                    if (mqttClient != null) mqttClient.stop();
                })
                .run(()-> {
                    if (isAwaitingConfirmation) {
                        twc.afterTextChanged(etConfirmationCode.getText());
                    } else {
                        twe.afterTextChanged(etEmail.getText());
                    }
                    recalcEnabledTrigger.toggle();
                })
                .show());

    }

    private void handleRegisterConfirm(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2) {
            boolean success = Boolean.parseBoolean(parts[0]);
            String info = parts[1];

            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    isAwaitingConfirmation = true;
                    etConfirmationCode.setVisibility(View.VISIBLE);
                    Toast.makeText(activity, "Please enter the code from your email", Toast.LENGTH_LONG).show();
                } else {
                    String errorText = "Server error: " + info;
                    Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
                    registrationDialogView.findViewById(R.id.etEmail).setEnabled(true);
                }
                dialogViewChild.setEnablePositive(false);
            });
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleRegisterResponse(String message, File baseDir) {
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

                        new MqttBase("ClientVerify",caCrt, myCrt, myKey) {
                            @Override
                            public void messageReceived(String topic, String message) {
                                super.messageReceived(topic, message);
                                if (topic.equals("/server/" + email + "/verify_confirm")) {
                                    activity.runOnUiThread(() -> {
                                        this.stop();
                                        dialogViewChild.cancel();
                                        try (InputStream myCrt = new FileInputStream(new File(certsDir, "my.crt"))){
                                            SharePerson newMe = CryptoUtils.getPersonData(myCrt);
                                            me.email = newMe.email;
                                            me.crt = newMe.crt;
                                            me.changed();
                                        } catch(Exception e){ mgLog.e(e.getMessage()); }
                                    });

                                }
                            }

                            @Override
                            protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                                if (!reconnect){
                                    this.registerTopic("/server/" + email + "/verify_confirm");
                                    this.sendMessage("/" + email + "/server/register_verify", "register_verify");

                                }
                            }
                        };
                    }

                } catch (Exception e) {
                    mgLog.e("Error processing register response", e);
                    success = false;
                    extra2 = e.getMessage();
                }
            }
            String finalExtra2 = extra2;
            if (!success){
                activity.runOnUiThread(() -> {
                    showError("Registration failed : "+ finalExtra2);
                    if (mqttClient != null){
                        mqttClient.stop();
                    }
                });
            }
        }
    }

    private void showError(String errorText){
        tvError.setText(errorText);
        tvError.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        recalcEnabledTrigger.changed();
    }

}
