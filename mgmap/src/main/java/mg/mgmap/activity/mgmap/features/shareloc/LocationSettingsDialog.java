package mg.mgmap.activity.mgmap.features.shareloc;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.mapsforge.map.layer.Layer;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import mg.mgmap.R;
import mg.mgmap.generic.view.DialogView;

public class LocationSettingsDialog {

    private final AppCompatActivity activity;
    private final String[] shareModes = {"Inactive", "Current GPS Session", "Always"};
    private static final String PREFS_NAME = "LocationPrefs";
    private static final String KEY_EMAILS = "known_emails";
    private static final String KEY_SHARE_WITH = "share_with_data";
    private static final String KEY_GET_FROM = "get_from_data";
    private static final String KEY_MASTER_SWITCH = "master_share_switch";

    private TextView tvOwnEmail;
    private TextView tvCertValidity;
    private Switch switchMasterShare;
    private Button btnRegisterInSettings;
    private LinearLayout layoutManageEmails;
    private ImageButton btnAddPersonShare;
    private ImageButton btnAddPersonGet;
//    private Button btnSaveSettings;
    private LinearLayout layoutShareWith;
    private LinearLayout layoutGetFrom;

    public LocationSettingsDialog(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void show() {

        DialogView dialogView = activity.findViewById(R.id.dialog_parent);


        View dialogViewX = activity.getLayoutInflater().inflate(R.layout.dialog_location_settings, null);

        tvOwnEmail = dialogViewX.findViewById(R.id.tvOwnEmail);
        tvCertValidity = dialogViewX.findViewById(R.id.tvCertValidity);
        switchMasterShare = dialogViewX.findViewById(R.id.switchMasterShare);
        btnRegisterInSettings = dialogViewX.findViewById(R.id.btnRegisterInSettings);
        layoutManageEmails = dialogViewX.findViewById(R.id.layoutManageEmails);
        btnAddPersonShare = dialogViewX.findViewById(R.id.btnAddPersonShare);
        btnAddPersonGet = dialogViewX.findViewById(R.id.btnAddPersonGet);
        layoutShareWith = dialogViewX.findViewById(R.id.layoutShareWith);
        layoutGetFrom = dialogViewX.findViewById(R.id.layoutGetFrom);

        updateCertificateInfo();
        loadSavedSettings();

//        AlertDialog dialog = new AlertDialog.Builder(activity)
//                .setView(dialogViewX)
//                .create();

        switchMasterShare.setOnCheckedChangeListener((buttonView, isChecked) -> updateEnabledState(isChecked));
        // Initial state update
        updateEnabledState(switchMasterShare.isChecked());

        layoutManageEmails.setOnClickListener(v -> showManageEmailsDialog());
        btnAddPersonShare.setOnClickListener(v -> showAddPersonDialog(layoutShareWith, true));
        btnAddPersonGet.setOnClickListener(v -> showAddPersonDialog(layoutGetFrom, false));


        btnRegisterInSettings.setOnClickListener(v -> {
//            dialog.dismiss();
            new Registration(activity).show(activity.getFilesDir());
        });

        dialogView.lock(() -> dialogView
                .setTitle("Share Location Settings")
                .setContentView(dialogViewX)
                .setPositive("OK", evt -> {
                    saveSettings();
                })
                .setNegative("Cancel", null)
                .show());


    }

    private void updateEnabledState(boolean enabled) {
        btnRegisterInSettings.setEnabled(enabled);
        layoutManageEmails.setEnabled(enabled);
        layoutManageEmails.setAlpha(enabled ? 1.0f : 0.5f);
        btnAddPersonShare.setEnabled(enabled);
        btnAddPersonGet.setEnabled(enabled);

        enableViewGroup(layoutShareWith, enabled);
        enableViewGroup(layoutGetFrom, enabled);
    }

    private void enableViewGroup(ViewGroup viewGroup, boolean enabled) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                enableViewGroup((ViewGroup) child, enabled);
            }
        }
        viewGroup.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void updateCertificateInfo() {
        File certFile = new File(activity.getFilesDir(), "certs/my.crt");
        if (certFile.exists()) {
            try (FileInputStream fis = new FileInputStream(certFile)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);

                X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
                RDN cn = x500name.getRDNs(BCStyle.CN)[0];
                String email = IETFUtils.valueToString(cn.getFirst().getValue());
                tvOwnEmail.setText("Email: " + email);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                tvCertValidity.setText("Certificate valid until: " + sdf.format(cert.getNotAfter()));

            } catch (Exception e) {
                Log.e("LocationSettingsDialog", "Error reading my.crt", e);
                tvOwnEmail.setText("Email: error reading cert");
                tvCertValidity.setText("Certificate valid until: N/A");
            }
        } else {
            tvOwnEmail.setText("Email: not registered");
            tvCertValidity.setText("Certificate valid until: N/A");
        }
    }

    private void loadSavedSettings() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        switchMasterShare.setChecked(prefs.getBoolean(KEY_MASTER_SWITCH, false));

        Set<String> shareWithSet = prefs.getStringSet(KEY_SHARE_WITH, new HashSet<>());
        for (String entry : shareWithSet) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                addShareWithItem(layoutShareWith, parts[0], Boolean.parseBoolean(parts[1]));
            }
        }

        Set<String> getFromSet = prefs.getStringSet(KEY_GET_FROM, new HashSet<>());
        for (String entry : getFromSet) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                addGetFromItem(layoutGetFrom, parts[0], Integer.parseInt(parts[1]));
            }
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(KEY_MASTER_SWITCH, switchMasterShare.isChecked());

        Set<String> shareWithSet = new HashSet<>();
        for (int i = 0; i < layoutShareWith.getChildCount(); i++) {
            View child = layoutShareWith.getChildAt(i);
            TextView tvEmail = child.findViewById(R.id.tvPersonEmail);
            CheckBox cbAlways = child.findViewById(R.id.cbAlways);
            if (tvEmail != null && cbAlways != null) {
                shareWithSet.add(tvEmail.getText().toString() + ":" + cbAlways.isChecked());
            }
        }
        editor.putStringSet(KEY_SHARE_WITH, shareWithSet);

        Set<String> getFromSet = new HashSet<>();
        for (int i = 0; i < layoutGetFrom.getChildCount(); i++) {
            View child = layoutGetFrom.getChildAt(i);
            TextView tvEmail = child.findViewById(R.id.tvPersonEmail);
            View colorPreview = child.findViewById(R.id.viewColorPreview);
            if (tvEmail != null && colorPreview != null) {
                int color = Color.GRAY;
                if (colorPreview.getBackground() instanceof ColorDrawable) {
                    color = ((ColorDrawable) colorPreview.getBackground()).getColor();
                }
                getFromSet.add(tvEmail.getText().toString() + ":" + color);
            }
        }
        editor.putStringSet(KEY_GET_FROM, getFromSet);

        editor.apply();
        Toast.makeText(activity, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void showManageEmailsDialog() {
        final SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> emailSet = prefs.getStringSet(KEY_EMAILS, new HashSet<>());
        List<String> emails = new ArrayList<>(emailSet);

        File certsDir = new File(activity.getFilesDir(), "certs");
        if (certsDir.exists() && certsDir.isDirectory()) {
            File[] files = certsDir.listFiles((dir, name) -> name.endsWith(".crt") && !name.equals("ca.crt") && !name.equals("my.crt"));
            if (files != null) {
                for (File f : files) {
                    String emailFromCert = f.getName().substring(0, f.getName().length() - 4);
                    if (!emails.contains(emailFromCert)) {
                        emails.add(emailFromCert);
                    }
                }
            }
        }

        final LinearLayout listLayout = new LinearLayout(activity);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(16, 16, 16, 16);

        for (String email : emails) {
            addEmailToManageList(listLayout, email);
        }

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(listLayout);

        final AlertDialog manageDialog = new AlertDialog.Builder(activity)
                .setTitle("Manage Email Addresses")
                .setView(scrollView)
                .setPositiveButton("Add New", null)
                .setNegativeButton("Close", (dialog, which) -> {
                    Set<String> updatedEmails = new HashSet<>();
                    for (int i = 0; i < listLayout.getChildCount(); i++) {
                        View child = listLayout.getChildAt(i);
                        TextView tv = child.findViewById(R.id.tvEmailAddress);
                        if (tv != null) updatedEmails.add(tv.getText().toString());
                    }
                    prefs.edit().putStringSet(KEY_EMAILS, updatedEmails).apply();
                })
                .create();

        manageDialog.setOnShowListener(dialog -> {
            Button addButton = manageDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            addButton.setOnClickListener(v -> showAddNewEmailDialog(listLayout));
        });

        manageDialog.show();
    }

    private void addEmailToManageList(LinearLayout container, String email) {
        View itemView = activity.getLayoutInflater().inflate(R.layout.item_manage_email, container, false);
        TextView tv = itemView.findViewById(R.id.tvEmailAddress);
        tv.setText(email);
        
        ImageButton btnRemove = itemView.findViewById(R.id.btnRemoveEmail);
        btnRemove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setMessage("Remove " + email + "?")
                .setPositiveButton("Remove", (d, w) -> container.removeView(itemView))
                .setNegativeButton("Cancel", null)
                .show());
        
        container.addView(itemView);
    }

    private void showAddNewEmailDialog(LinearLayout manageContainer) {
        final EditText et = new EditText(activity);
        et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        et.setHint("Email address");

        final AlertDialog addEmailDialog = new AlertDialog.Builder(activity)
                .setTitle("Add Email")
                .setView(et)
                .setPositiveButton("Add", (d, w) -> {
                    String newEmail = et.getText().toString().trim();
                    boolean exists = false;
                    for (int i = 0; i < manageContainer.getChildCount(); i++) {
                        TextView tv = manageContainer.getChildAt(i).findViewById(R.id.tvEmailAddress);
                        if (tv != null && tv.getText().toString().equalsIgnoreCase(newEmail)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        addEmailToManageList(manageContainer, newEmail);
                    } else {
                        Toast.makeText(activity, "Email already in list", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                boolean isValid = Patterns.EMAIL_ADDRESS.matcher(input).matches();
                Button addButton = addEmailDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (addButton != null) {
                    addButton.setEnabled(isValid);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addEmailDialog.show();
        addEmailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void showAddPersonDialog(LinearLayout container, boolean isShare) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> emailSet = new HashSet<>(prefs.getStringSet(KEY_EMAILS, new HashSet<>()));
        
        File certsDir = new File(activity.getFilesDir(), "certs");
        if (certsDir.exists() && certsDir.isDirectory()) {
            File[] files = certsDir.listFiles((dir, name) -> name.endsWith(".crt") && !name.equals("ca.crt") && !name.equals("my.crt"));
            if (files != null) {
                for (File f : files) {
                    emailSet.add(f.getName().substring(0, f.getName().length() - 4));
                }
            }
        }

        Set<String> existingEmails = new HashSet<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView tv = child.findViewById(R.id.tvPersonEmail);
            if (tv != null) {
                existingEmails.add(tv.getText().toString().toLowerCase());
            }
        }
        
        List<String> availableEmails = new ArrayList<>();
        for (String email : emailSet) {
            if (!existingEmails.contains(email.toLowerCase())) {
                availableEmails.add(email);
            }
        }

        if (availableEmails.isEmpty()) {
            Toast.makeText(activity, "No more email addresses available to add", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] emailsArray = availableEmails.toArray(new String[0]);

        new AlertDialog.Builder(activity)
                .setTitle(isShare ? "Share with ..." : "Get from ...")
                .setItems(emailsArray, (dialog, which) -> {
                    String selectedEmail = emailsArray[which];
                    if (isShare) {
                        addShareWithItem(container, selectedEmail, false);
                    } else {
                        addGetFromItem(container, selectedEmail, Color.GRAY);
                    }
                })
                .show();
    }

    private void addShareWithItem(LinearLayout container, String email, boolean always) {
        View itemView = activity.getLayoutInflater().inflate(R.layout.item_person_share, container, false);
        ((TextView) itemView.findViewById(R.id.tvPersonEmail)).setText(email);
        
        CheckBox cbAlways = itemView.findViewById(R.id.cbAlways);
        cbAlways.setChecked(always);
        
        itemView.findViewById(R.id.btnRemovePerson).setOnClickListener(v -> container.removeView(itemView));
        container.addView(itemView);
        
        if (!switchMasterShare.isChecked()) {
            enableViewGroup((ViewGroup) itemView, false);
        }
    }

    private void addGetFromItem(LinearLayout container, String email, int color) {
        View itemView = activity.getLayoutInflater().inflate(R.layout.item_person_get, container, false);
        ((TextView) itemView.findViewById(R.id.tvPersonEmail)).setText(email);
        
        View colorPreview = itemView.findViewById(R.id.viewColorPreview);
        colorPreview.setBackgroundColor(color);
        colorPreview.setOnClickListener(v -> showColorPickerDialog(colorPreview));

        itemView.findViewById(R.id.btnRemovePerson).setOnClickListener(v -> container.removeView(itemView));
        container.addView(itemView);

        if (!switchMasterShare.isChecked()) {
            enableViewGroup((ViewGroup) itemView, false);
        }
    }

    private void showColorPickerDialog(View colorPreview) {
        final String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Black", "Gray"};
        final int[] colorValues = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.BLACK, Color.GRAY};

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() { return colorNames.length; }
            @Override
            public Object getItem(int position) { return colorValues[position]; }
            @Override
            public long getItemId(int position) { return position; }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater().inflate(R.layout.item_color_picker, parent, false);
                }
                View box = convertView.findViewById(R.id.viewColorBox);
                TextView name = convertView.findViewById(R.id.tvColorName);
                box.setBackgroundColor(colorValues[position]);
                name.setText(colorNames[position]);
                return convertView;
            }
        };

        new AlertDialog.Builder(activity)
                .setTitle("Select Color")
                .setAdapter(adapter, (dialog, which) -> colorPreview.setBackgroundColor(colorValues[which]))
                .show();
    }
}
