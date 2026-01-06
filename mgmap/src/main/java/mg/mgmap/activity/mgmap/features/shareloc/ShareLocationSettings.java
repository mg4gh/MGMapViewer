package mg.mgmap.activity.mgmap.features.shareloc;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.SystemClock;
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
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import mg.mgmap.R;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;

public class ShareLocationSettings {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    SimpleDateFormat sdf = new SimpleDateFormat(FSShareLoc.DATE_FORMAT, Locale.getDefault());

    private final AppCompatActivity activity;

    private TextView tvOwnEmail;
    private TextView tvOwnValidity;
    private ImageButton btnAddPerson;
    private LinearLayout btnRegistrationParent;
    private LinearLayout layoutShareWithFrom;

    private final ShareLocConfig shareLocConfig;
    private final ShareLocConfig config;
    private final SharePerson me;

    public ShareLocationSettings(AppCompatActivity activity, ShareLocConfig shareLocConfig, SharePerson me) {
        this.activity = activity;
        this.shareLocConfig = shareLocConfig;
        this.config = new ShareLocConfig(shareLocConfig); // deep copy
        this.me = me;
    }

    @SuppressLint("SetTextI18n")
    public void show() {
        @SuppressLint({"InflateParams"})
        View locationSettingsDialogView = activity.getLayoutInflater().inflate(R.layout.dialog_location_settings, null, false);

        DialogView dialogView = new DialogView(activity){
            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                me.deleteObservers();
            }
        };

        tvOwnEmail = locationSettingsDialogView.findViewById(R.id.tvOwnEmail);
        tvOwnValidity = locationSettingsDialogView.findViewById(R.id.tvCertValidity);

        btnRegistrationParent = locationSettingsDialogView.findViewById(R.id.btnRegistrationParent);
        ExtendedTextView btnRegister = dialogView.createButton("",R.id.bt_registration, "bt_register",
                pce->new Registration(activity, me).show(activity.getFilesDir()));
        btnRegistrationParent.addView(btnRegister);
        ExtendedTextView btnUnregister = dialogView.createButton("Unregister",R.id.bt_registration, "bt_unregister",
                pce->unregister(activity.getFilesDir()));
        btnRegistrationParent.addView(btnUnregister);
        btnAddPerson = locationSettingsDialogView.findViewById(R.id.btnAddPerson);
        layoutShareWithFrom = locationSettingsDialogView.findViewById(R.id.layoutShareWithFrom);

        me.addObserver(pce->{
            tvOwnEmail.setText("Email: "+me.email);
            setCertificateValidity(tvOwnValidity, me, System.currentTimeMillis(), true);
            boolean isRegistered = !me.email.equals(SharePerson.DUMMY_EMAIL);
            btnRegister.setText(!isRegistered?"Register":"Refresh");
            btnUnregister.setEnabled(isRegistered);
        });
        me.changed();
        loadSavedSettings();

        btnAddPerson.setBackgroundColor(0xFF0000);

        btnAddPerson.setOnClickListener(v -> {
            SharePerson person = new SharePerson();
            config.persons.add(person);
            showAddPerson(layoutShareWithFrom, person);
        });

        List<SharePerson> tempPersons = new ArrayList<>();
        for (SharePerson person : config.persons){
            if (person.crt.startsWith("network error")){
                tempPersons.add(person);
            }
        }
        if (!tempPersons.isEmpty()){
            MqttUtil.updateCertificate(activity, me, tempPersons);
        }

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

    private void loadSavedSettings() {
        layoutShareWithFrom.removeAllViews();
        for (SharePerson person : config.persons) {
            showAddPerson(layoutShareWithFrom, person);
        }
    }

    private boolean saveSettings() {
        SharePerson me;
        try (InputStream clientCrt = new FileInputStream(new File(activity.getFilesDir(), "certs/my.crt")) ) {
            me = CryptoUtils.getPersonData(clientCrt);
            if (me.shareWithUntil < 10*24*60*60*1000L + System.currentTimeMillis()){
                Toast.makeText(activity, "ERROR: refresh certificate", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(activity, "ERROR: register first", Toast.LENGTH_LONG).show();
            return false;
        }


        HashSet<String> emails = new HashSet<>();
        for (SharePerson person : config.persons){
            emails.add(person.email);
            if (person.email.equals(SharePerson.DUMMY_EMAIL)){
                Toast.makeText(activity, "ERROR: invalid email address", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        if (config.persons.size() != emails.size()){
            Toast.makeText(activity, "ERROR: duplicate email address", Toast.LENGTH_LONG).show();
            return false;
        }
        // remove observers that were used to refresh UI
        for (SharePerson person : config.persons) {
            person.deleteObservers();
        }
        shareLocConfig.persons = config.persons; // replace with modified config persons
        shareLocConfig.saveConfig();
        shareLocConfig.changed();
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void setCertificateValidity(TextView tvValidity, SharePerson person, long now, boolean withDate){
        try {
            if ((person.crt != null) && (!person.crt.isEmpty())){
                if ("unknown".equals(person.crt)) {
                    tvValidity.setTextColor(CC.getColor(R.color.CC_RED));
                    tvValidity.setText("Certificate not found");
                } else if (person.crt.startsWith("network error")) {
                    tvValidity.setTextColor(CC.getColor(R.color.CC_RED));
                    tvValidity.setText("Certificate network problem");
                } else {
                    SharePerson p = CryptoUtils.getPersonData(new ByteArrayInputStream(person.crt.getBytes()));
                    String sWithDate = withDate?(": "+sdf.format(p.shareWithUntil)):"";
                    if  (p.shareWithUntil < now){ // certificate is no longer valid
                        tvValidity.setTextColor(CC.getColor(R.color.CC_RED));
                        tvValidity.setText("Certificate expired"+sWithDate);
                    } else if  (p.shareWithUntil < now + 30*24*60*60*1000L){ // certificate will expire in the next 30 days
                        tvValidity.setTextColor(CC.getColor(R.color.CC_ORANGE));
                        tvValidity.setText("Certificate expires soon"+sWithDate);
                    } else {
                        tvValidity.setTextColor(CC.getColor(R.color.CC_GREEN150_A150));
                        tvValidity.setText("Certificate valid"+sWithDate);
                    }
                }
            } else {
                tvValidity.setTextColor(CC.getColor(R.color.CC_RED));
                tvValidity.setText("Certificate not found");
            }
        } catch (Exception e) {
            mgLog.e(e);
            tvValidity.setTextColor(CC.getColor(R.color.CC_RED));
            tvValidity.setText("Certificate error");
        }
    }


    @SuppressLint("SetTextI18n")
    private void showAddPerson(LinearLayout container, SharePerson person) {
        View itemView = activity.getLayoutInflater().inflate(R.layout.shareloc_person_other, container, false);

        TextView tvEmail = itemView.findViewById(R.id.tvPersonEmail);
        tvEmail.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvEmail.setOnClickListener(view -> editEmail(person));
        TextView tvCrtValidity = itemView.findViewById(R.id.tvCertValidity);
        ImageButton btnRemovePerson = itemView.findViewById(R.id.btnRemoveItem);
        btnRemovePerson.setOnClickListener(v->{
            container.removeView(itemView);
            config.persons.removeIf(p->tvEmail.getText().toString().equals(p.email));
        });

        long now = System.currentTimeMillis();
        if (now > person.shareWithUntil) person.shareWithActive = false;
        CheckBox cbShareWithOn = itemView.findViewById(R.id.cbShareWithOn);
        TextView tvShareWithUntil = itemView.findViewById(R.id.tvShareWithUntil);
        tvShareWithUntil.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvShareWithUntil.setOnClickListener(v->editUntil(person, true));

        if (now > person.shareFromUntil) person.shareFromActive = false;
        CheckBox cbShareFromOn = itemView.findViewById(R.id.cbShareFromOn);
        TextView tvShareFromUntil = itemView.findViewById(R.id.tvShareFromUntil);
        tvShareFromUntil.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.shape2, activity.getTheme()));
        tvShareFromUntil.setOnClickListener(v->editUntil(person, false));

        View viewColorPreview = itemView.findViewById(R.id.viewColorPreview);
        viewColorPreview.setBackgroundColor(person.color);
        viewColorPreview.setOnClickListener(v->showColorPickerDialog(person));

        person.addObserver(pce-> activity.runOnUiThread(() ->{
            tvEmail.setText(person.email);
            tvShareWithUntil.setText( (person.shareWithUntil == Long.MAX_VALUE)?"infinite":sdf.format(new Date(person.shareWithUntil)) );
            cbShareWithOn.setChecked(person.shareWithActive);
            tvShareFromUntil.setText( sdf.format(new Date(person.shareFromUntil)) );
            cbShareFromOn.setChecked(person.shareFromActive);
            viewColorPreview.setBackgroundColor(person.color);
            setCertificateValidity(tvCrtValidity, person, now, false);
        }));
        person.changed();
        container.addView(itemView);
    }

    private void editEmail(SharePerson person){
        DialogView dialogViewChild = new DialogView(activity);
        Pref<Boolean> enablePref = new Pref<>(false);
        enablePref.addObserver(pce->dialogViewChild.setEnablePositive(enablePref.getValue()));

        EditText etEmail = new EditText(activity);
        etEmail.setText(person.email);
        etEmail.setSelectAllOnFocus(true);
        etEmail.requestFocus();
        TextWatcherEmail twe = new TextWatcherEmail(enablePref);
        etEmail.addTextChangedListener(twe);
        enablePref.addObserver(pce->dialogViewChild.setEnablePositive(enablePref.getValue()));

        dialogViewChild.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Enter email address")
                .setContentView(etEmail)
                .setPositive("Ok",pce->{
                    person.email = etEmail.getText().toString();
                    person.changed();
                    MqttUtil.updateCertificate(activity, me, List.of(person));
                })
                .setNegative("Cancel",null)
                .run(()-> {
                    twe.afterTextChanged(etEmail.getText());
                    enablePref.changed();
                })
                .show());
    }

    @SuppressLint("SetTextI18n")
    private void editUntil(SharePerson person, boolean isShareWithUntil){
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
                    person.changed();
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

    private void showColorPickerDialog(SharePerson person) {
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
                person.changed();
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

    private void unregister(File baseDir){
        DialogView dialogViewChild = new DialogView(activity);
        dialogViewChild.lock(() -> dialogViewChild
                .setTitle("Unregister Confirmation")
                .setMessage("You are about to unregister your email address. Location sharing is not longer possible after unregister.")
                .setPositive("Confirm", pce->doUnregister(baseDir))
                .setNegative("Cancel", null)
                .show());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void doUnregister(File baseDir){
        MqttUtil.unregister(activity, me);
        SystemClock.sleep(1000);
        File certsDir = new File(baseDir, "certs");
        File fCrt = new File(certsDir, "my.crt");
        if (fCrt.exists()) fCrt.delete();
        File fKey = new File(certsDir, "my.key");
        if (fKey.exists()) fKey.delete();
        me.init();
        me.changed();
    }
 }
