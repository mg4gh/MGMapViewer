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

        durationLimit = 60 * 1000;

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

        boolean[] loop = new boolean[1];
        loop[0] = true;
        WaitUtil.doWait(TestControl.class, 1000);
        while (loop[0]){
            mgMapActivity.runOnUiThread(() -> {
                loop[0] = running && !(mgMapApplication.confirmAlertDialogs(mgMapActivity)); // successful confirm Download finished alert Dialog
                mgLog.d("check download finished: "+(!loop[0]));
            });
            WaitUtil.doWait(TestControl.class, 1000);
        }
        WaitUtil.doWait(TestControl.class, 2000);
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.confirmAlertDialogs(testControl.getActivity(SettingsActivity.class)); // confirm Map layer assignment hint alert Dialog
        });
        testControl.setCursorVisibility(true);
        WaitUtil.doWait(TestControl.class, 1000);
        animateTo(testControl.getPreferenceCenter(R.string.Layers_pref_chooseMap2_key), 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 2000);

        testControl.handleSettingsDialog(R.string.Layers_pref_chooseMap2_key,"MAPSFORGE: Berlin_oam.osm.map");
        WaitUtil.doWait(TestControl.class, 5000);
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("click on: Got it!"); // hint_initial_map_download_key is already visible at start of test
        addRegex("onClick group_task");
        addRegex("onClick download");
        addRegex("onPreferenceClick https://www.openandromaps.org/downloads/deutschland");
        addRegex("showHint key=hint_initial_map_download2_key");
        addRegex("click on: Got it!"); // confirm hint_initial_map_download2_key
        addRegex("Intent . act=android.intent.action.VIEW dat=mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip");
        addRegex("click on: YES"); // confirm download
        addRegex("BgJobUtil.* do it");
        addRegex("BgJobGroup.jobFinished.* successCounter=1  errorCounter=0  jobCounter=1");
        addRegex("click on: OK"); // confirm download finished
        addRegex("showHint key=hint_map_layer_assignment_key");
        addRegex("click on: Got it!"); // confirm hint_map_layer_assignment_key
        addRegex("MapLayerListPreference.onClick.* key=SelectMap2 value=none");
        addRegex("recreate MGMapActivity due to key=SelectMap2 value=MAPSFORGE: Berlin_oam.osm.map");
        addRegex(getName()+" stop");
    }

}
