package mg.mgmap.activity.mgmap.features.shareloc;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import mg.mgmap.R;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;

public class ShareLocationSettings {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());

    private final AppCompatActivity activity;
    private static final String PREFS_NAME = "ShareLocationPrefs";
    private static final String KEY_SHARE_PERSONS = "key_share_persons";
    private static final String KEY_MASTER_SWITCH = "master_share_switch";

    private TextView tvOwnEmail;
    private TextView tvCertValidity;
    private Switch switchMasterShare;
    private ImageButton btnAddPerson;
//    private Button btnSaveSettings;
    private LinearLayout btnRegistrationParent;
    private ExtendedTextView btnRegistration;
    private LinearLayout layoutShareWithFrom;

    private final ArrayList<SharePerson> persons = new ArrayList<>();

    public ShareLocationSettings(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void show() {

//        DialogView dialogView = activity.findViewById(R.id.dialog_parent);
        DialogView dialogView = new DialogView(activity);


        View locationSettingsDialogView = activity.getLayoutInflater().inflate(R.layout.dialog_location_settings, null);

        tvOwnEmail = locationSettingsDialogView.findViewById(R.id.tvOwnEmail);
        tvCertValidity = locationSettingsDialogView.findViewById(R.id.tvCertValidity);
        switchMasterShare = locationSettingsDialogView.findViewById(R.id.switchMasterShare);
        btnRegistrationParent = locationSettingsDialogView.findViewById(R.id.btnRegistration);
        btnRegistration = dialogView.createButton("Register / Refresh certificate",R.id.bt_registration, "bt_register",
                pce->new Registration(activity).show(activity.getFilesDir(), this));
        btnRegistrationParent.addView(btnRegistration);
        btnAddPerson = locationSettingsDialogView.findViewById(R.id.btnAddPerson);
        layoutShareWithFrom = locationSettingsDialogView.findViewById(R.id.layoutShareWithFrom);

        updateCertificateInfo();
        loadSavedSettings();

        switchMasterShare.setOnCheckedChangeListener((buttonView, isChecked) -> updateEnabledState(isChecked));
        // Initial state update
        updateEnabledState(switchMasterShare.isChecked());

        btnAddPerson.setBackgroundColor(0xFF0000);

        btnAddPerson.setOnClickListener(v -> {
            SharePerson person = new SharePerson();
            persons.add(person);
            showAddPerson(layoutShareWithFrom, person);
        });

        dialogView.lock(() -> dialogView
                .setTitle("Share Location Settings")
                .setContentView(locationSettingsDialogView)
                .setPositive("Save", evt -> {
                    if (saveSettings()){
                        dialogView.cancel();
                    }
                }, false)
                .setNegative("Cancel", null)
                .show());


    }

    private void updateEnabledState(boolean enabled) {
        btnRegistration.setEnabled(enabled);
        btnAddPerson.setEnabled(enabled);

        enableViewGroup(layoutShareWithFrom, enabled);
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

    void updateCertificateInfo() {
        File certFile = new File(activity.getFilesDir(), "certs/my.crt");
        if (certFile.exists()) {
            try (FileInputStream fis = new FileInputStream(certFile)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);

                X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
                RDN cn = x500name.getRDNs(BCStyle.CN)[0];
                String email = IETFUtils.valueToString(cn.getFirst().getValue());
                tvOwnEmail.setText("Email: " + email);

                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm", Locale.getDefault());
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
        Set<String> shareWithSet = prefs.getStringSet(KEY_SHARE_PERSONS, new HashSet<>());
        persons.clear();
        layoutShareWithFrom.removeAllViews();
        for (String entry : shareWithSet) {
            SharePerson person = SharePerson.fromPrefString(entry);
            persons.add(person);
            showAddPerson(layoutShareWithFrom, person);
        }
    }

    private boolean saveSettings() {
        HashSet<String> emails = new HashSet<>();
        for (SharePerson person : persons){
            emails.add(person.email);
            if (person.email.equals(SharePerson.DUMMY_EMAIL)){
                Toast.makeText(activity, "ERROR: invalid email address", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        if (persons.size() != emails.size()){
            Toast.makeText(activity, "ERROR: duplicate email address", Toast.LENGTH_LONG).show();
            return false;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_MASTER_SWITCH, switchMasterShare.isChecked());
        Set<String> shareWithSet = new HashSet<>();
        for (SharePerson person : persons){
            shareWithSet.add(person.toPrefString());
        }
        editor.putStringSet(KEY_SHARE_PERSONS, shareWithSet);
        editor.apply();
        Toast.makeText(activity, "Settings saved", Toast.LENGTH_SHORT).show();
        return true;
    }




    private void showAddPerson(LinearLayout container, SharePerson person) {
        View itemView = activity.getLayoutInflater().inflate(R.layout.item_shareloc, container, false);

        TextView tvEmail = itemView.findViewById(R.id.tvPersonEmail);
        tvEmail.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvEmail.setText(person.email);
        tvEmail.setOnClickListener(view -> editEmail(tvEmail, person));

        ImageButton btnRemovePerson = itemView.findViewById(R.id.btnRemoveItem);
        btnRemovePerson.setOnClickListener(v->{
            container.removeView(itemView);
            persons.removeIf(p->tvEmail.getText().toString().equals(p.email));
        });

        long now = System.currentTimeMillis();
        if (now > person.shareWithUntil) person.shareWithActive = false;
        CheckBox cbShareWithOn = itemView.findViewById(R.id.cbShareWithOn);
        cbShareWithOn.setChecked(person.shareWithActive);
        TextView tvShareWithUntil = itemView.findViewById(R.id.tvShareWithUntil);
        tvShareWithUntil.setText( (person.shareWithUntil == Long.MAX_VALUE)?"infinite":sdf.format(new Date(person.shareWithUntil)) );
        tvShareWithUntil.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvShareWithUntil.setOnClickListener(v->editUntil(tvShareWithUntil, cbShareWithOn, person, true));

        if (now > person.shareFromUntil) person.shareFromActive = false;
        CheckBox cbShareFromOn = itemView.findViewById(R.id.cbShareFromOn);
        cbShareFromOn.setChecked(person.shareFromActive);
        TextView tvShareFromUntil = itemView.findViewById(R.id.tvShareFromUntil);
        tvShareFromUntil.setText( sdf.format(new Date(person.shareFromUntil)) );
        tvShareFromUntil.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvShareFromUntil.setOnClickListener(v->editUntil(tvShareFromUntil, cbShareFromOn, person, false));

        View viewColorPreview = itemView.findViewById(R.id.viewColorPreview);
        viewColorPreview.setBackgroundColor(person.color);
        viewColorPreview.setOnClickListener(v->showColorPickerDialog(viewColorPreview, person));


        if (!switchMasterShare.isChecked()) {
            enableViewGroup((ViewGroup) itemView, false);
        }
        container.addView(itemView);
    }

    private void editEmail(TextView tvEmail, SharePerson person){
        DialogView dialogViewChild = new DialogView(activity);

        EditText etEmail = new EditText(activity);
        etEmail.setText(tvEmail.getText());
        etEmail.setSelectAllOnFocus(true);
        etEmail.requestFocus();
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                boolean isValid = Patterns.EMAIL_ADDRESS.matcher(input).matches();
                dialogViewChild.setEnablePositive(isValid);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialogViewChild.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Enter email address")
                .setContentView(etEmail)
                .setPositive("Ok",pce->{
                    person.email = etEmail.getText().toString();
                    tvEmail.setText(person.email);
                })
                .setNegative("Cancel",null)
                .show());
    }

    private void editUntil(TextView tvUntil, CheckBox cbUntil, SharePerson person, boolean isShareWithUntil){
        Calendar cal = Calendar.getInstance();
        long timestamp = isShareWithUntil?person.shareWithUntil:person.shareFromUntil;
        long now = System.currentTimeMillis();
        long defaultUntilTime = now + 60 * 60 * 1000L; // default due time is now + 1 hour
        if (timestamp < defaultUntilTime) {
            timestamp = defaultUntilTime;
        }
        cal.setTimeInMillis(timestamp);

        DialogView dialogViewChild = new DialogView(activity);



        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout llDate = new LinearLayout(activity);
        TextView tvDate;
        {
            llDate.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            llDate.setOrientation(LinearLayout.HORIZONTAL);

            Space space1 = new Space(activity);
            LinearLayout.LayoutParams llSpace1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            llSpace1.weight = 1;
            space1.setLayoutParams(llSpace1);
            llDate.addView(space1);

            TextView tvDateName = new TextView(activity);
            LinearLayout.LayoutParams lpDateName = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tvDateName.setPadding(VUtil.dp(10),VUtil.dp(10),VUtil.dp(10),VUtil.dp(10));
            lpDateName.setMargins(VUtil.dp(2),VUtil.dp(10),VUtil.dp(2),VUtil.dp(10));
            tvDateName.setLayoutParams(lpDateName);
            tvDateName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            tvDateName.setText("Date: ");
            llDate.addView(tvDateName);

            tvDate = new TextView(activity);
            LinearLayout.LayoutParams lpDate = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tvDate.setPadding(VUtil.dp(10),VUtil.dp(10),VUtil.dp(10),VUtil.dp(10));
            lpDate.setMargins(VUtil.dp(2),VUtil.dp(10),VUtil.dp(2),VUtil.dp(10));
            tvDate.setLayoutParams(lpDate);
            tvDate.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            tvDate.setOnClickListener(v->editUntilDate(tvDate,cal));
            tvDate.setGravity(Gravity.CENTER);
            llDate.addView(tvDate);

            Space space2 = new Space(activity);
            LinearLayout.LayoutParams llSpace2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            llSpace2.weight = 1;
            space2.setLayoutParams(llSpace2);
            llDate.addView(space2);

            linearLayout.addView(llDate);

        }

        TimePicker timePicker = new TimePicker(activity);
        timePicker.setIs24HourView(true);
        timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(cal.get(Calendar.MINUTE));
        linearLayout.addView(timePicker);

        setTextViewDate(tvDate, timePicker, cal);


        if (isShareWithUntil) {
            LinearLayout llInfinity = new LinearLayout(activity);
            llInfinity.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            llInfinity.setOrientation(LinearLayout.HORIZONTAL);

            Space space1 = new Space(activity);
            LinearLayout.LayoutParams llSpace1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            llSpace1.weight = 1;
            space1.setLayoutParams(llSpace1);
            llInfinity.addView(space1);

            TextView tvDateName = new TextView(activity);
            LinearLayout.LayoutParams lpDateName = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tvDateName.setPadding(VUtil.dp(10),VUtil.dp(10),VUtil.dp(10),VUtil.dp(10));
            lpDateName.setMargins(VUtil.dp(2),VUtil.dp(10),VUtil.dp(2),VUtil.dp(10));
            tvDateName.setLayoutParams(lpDateName);
            tvDateName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            tvDateName.setText("Infinite: ");
            llInfinity.addView(tvDateName);

            CheckBox cbInfinite = new CheckBox(activity);
            LinearLayout.LayoutParams lpInfinite = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cbInfinite.setPadding(VUtil.dp(10),VUtil.dp(10),VUtil.dp(10),VUtil.dp(10));
            lpInfinite.setMargins(VUtil.dp(2),VUtil.dp(10),VUtil.dp(2),VUtil.dp(10));
            cbInfinite.setLayoutParams(lpInfinite);
            cbInfinite.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            llInfinity.addView(cbInfinite);
            Pref<Boolean> prefCbInfinity = new Pref<>(true);
            prefCbInfinity.addObserver(pcl->{
                mgLog.d("Checked="+cbInfinite.isChecked());
                enableViewGroup(llDate, !cbInfinite.isChecked());
                enableViewGroup(timePicker, !cbInfinite.isChecked());
                if (cbInfinite.isChecked()){
                    cal.setTimeInMillis(Long.MAX_VALUE);
                } else {
                    if (cal.getTimeInMillis() == Long.MAX_VALUE){
                        cal.setTimeInMillis(defaultUntilTime);
                        setTextViewDate(tvDate, timePicker, cal);
                    }
                }
            });
            cbInfinite.setOnClickListener((v)->prefCbInfinity.toggle());
            Space space2 = new Space(activity);
            LinearLayout.LayoutParams llSpace2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            llSpace2.weight = 1;
            space2.setLayoutParams(llSpace2);
            llInfinity.addView(space2);
            linearLayout.addView(llInfinity);
            cbInfinite.setChecked(timestamp == Long.MAX_VALUE);
            prefCbInfinity.toggle();
        }




        dialogViewChild.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Enter due date for share "+(isShareWithUntil?"with":"from"))
                .setContentView(linearLayout)
                .setPositive("OK",pce->{
                    boolean infinite = (cal.getTimeInMillis() == Long.MAX_VALUE);
                    if (!infinite){
                        cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                        cal.set(Calendar.MINUTE, timePicker.getMinute());
                    }
                    long ts = cal.getTimeInMillis();
                    boolean checked = (ts > now);
                    if (isShareWithUntil){
                        person.shareWithUntil = ts;
                        person.shareWithActive = checked;
                    } else {
                        person.shareFromUntil = ts;
                        person.shareFromActive = checked;
                    }
                    cbUntil.setChecked(checked);

                    tvUntil.setText(infinite?"infinite":sdf.format(ts));
                })
                .setNegative("Cancel",null)
                .show());

    }

    private void setTextViewDate(TextView tvDate, TimePicker timePicker, Calendar cal){
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
        tvDate.setText( sdfDate.format(cal.getTime()) );
        if (timePicker != null){
            timePicker.setIs24HourView(true);
            timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(cal.get(Calendar.MINUTE));
        }
    }

    private void editUntilDate(TextView tvDate, Calendar cal){
        DialogView dialogViewChild = new DialogView(activity);
        DatePicker datePicker = new DatePicker(activity);
        datePicker.init(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH),null);
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Select date")
                .setContentView(datePicker)
                .setPositive("Ok",pce->{
                    cal.set(Calendar.YEAR, datePicker.getYear());
                    cal.set(Calendar.MONTH, datePicker.getMonth());
                    cal.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                    setTextViewDate(tvDate, null, cal);
                })
                .setNegative("Cancel",null)
                .show());
    }

    private void showColorPickerDialog(View colorPreview, SharePerson person) {
        final String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Black", "Gray", "Orange"};
        final int[] colorValues = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.BLACK, Color.GRAY, CC.getColor(R.color.CC_ORANGE)};

        LinearLayout llColors = new LinearLayout(activity);
        llColors.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        llColors.setOrientation(LinearLayout.VERTICAL);

        DialogView dialogViewChild = new DialogView(activity);
        for (int i=0; i<colorNames.length; i++){
            int colorValue = colorValues[i];
            View llColor = activity.getLayoutInflater().inflate(R.layout.item_color_picker, llColors, false);
            View box = llColor.findViewById(R.id.viewColorBox);
            TextView name = llColor.findViewById(R.id.tvColorName);
            box.setBackgroundColor(colorValue);
            name.setText(colorNames[i]);
            llColor.setOnClickListener(v->{
                person.color = colorValue;
                colorPreview.setBackgroundColor(colorValue);
                dialogViewChild.cancel();
            });
            if (person.color == colorValue){
                llColor.setBackgroundColor(CC.getColor(R.color.CC_GRAY240));
            }
            llColors.addView(llColor);
        }

        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Select color")
                .setMessage("Select a color that is used to visualize the shared position")
                .setContentView(llColors)
                .show());


    }
}
