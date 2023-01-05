package mg.mgmap.test.tc.init;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;
import mg.mgmap.test.TestControl;

@SuppressWarnings("unused")
public class Init1 extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public Init1(MGMapApplication mgMapApplication) {
        super(mgMapApplication);
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void run() {
        super.run();

        MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
        if (mgMapActivity == null) return; // runs in background - do nothing

        setCursorPosition(getCenterPosition());
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(mgMapActivity); // confirm HintInitialMapDownload
        });

        animateTo(testControl.getViewClickPos("group_task"), 1000);
        doClick();
        animateTo(testControl.getViewClickPos("download"), 1000);
        doClick();

        WaitUtil.doWait(TestControl.class, 300);
        animateTo(testControl.getPreferenceCenter(R.string.preferences_dl_maps_de_key), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        doClick();

        animateTo(getCenterPosition(), 1000);
        WaitUtil.doWait(TestControl.class, 2000); // now you see HintInitialMapDownload2
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(testControl.getActivity(SettingsActivity.class)); // confirm hint alertDialog, which triggers the Intent to open the Browser with the maps download page
        });
        WaitUtil.doWait(TestControl.class, 2000);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip"));
        testControl.getActivity(SettingsActivity.class).startActivity(intent);
        WaitUtil.doWait(TestControl.class, 2000);
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(mgMapActivity); // confirm start Download alert Dialog
        });
        WaitUtil.doWait(TestControl.class, 10000);
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(mgMapActivity); // confirm Download finished alert Dialog
        });
        WaitUtil.doWait(TestControl.class, 2000);
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(testControl.getActivity(SettingsActivity.class)); // confirm Map layer assignment hint alert Dialog
        });
        testControl.setCursorVisibility(true);
        WaitUtil.doWait(TestControl.class, 1000);
        animateTo(testControl.getPreferenceCenter(R.string.Layers_pref_chooseMap2_key), 1000);
        doClick();
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("click on: Got it!"); // hint_initial_map_download_key is already visible at start of test
        addRegex("onClick group_task");
        addRegex("onClick download");
        addRegex(getName()+" stop");
    }

}
