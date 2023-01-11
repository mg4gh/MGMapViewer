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
import mg.mgmap.generic.view.DialogView;
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

        // start test
        if (testControl.getActivity(MGMapActivity.class) == null) return;
        setCursorPosition(getCenterPosition());

        // confirm HintInitialMapDownload
        animateTo(testControl.getViewClickPos("HintInitialMapDownload_btPositive"), 1000);
        doClick();

        // open download setting
        animateTo(testControl.getViewClickPos("group_task"), 1000);
        doClick();
        animateTo(testControl.getViewClickPos("download"), 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 300);

        // open download maps germany
        animateTo(testControl.getPreferenceCenter(R.string.preferences_dl_maps_de_key), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        doClick();

        // confirm HintInitialMapDownload2 (contains instruction for openandromaps webpage)
        WaitUtil.doWait(TestControl.class, 1000);
        animateTo(testControl.getViewClickPos("HintInitialMapDownload2_btPositive"), 1000);
        doClick();

        // simulate click in openandromaps webpage with selection of berlin map
        WaitUtil.doWait(TestControl.class, 2000); // now you see the browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip"));
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        testControl.getActivity(SettingsActivity.class).startActivity(intent);
        WaitUtil.doWait(TestControl.class, 500);
        testControl.setCursorVisibility(true);
        WaitUtil.doWait(TestControl.class, 1000);
        testControl.setCursorVisibility(true);

        // now MGMapActivity is again open - confirm download request for map
        animateTo(testControl.getViewClickPos("bgJobGroupConfirm_Download_map_btPositive"), 500);
        doClick();

        // wait for download finished (check for dialog existence) - that indicates end of download
        boolean[] loop = new boolean[1];
        WaitUtil.doWait(TestControl.class, 1000);
        MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
        if (mgMapActivity == null) return; // runs in background - do nothing
        DialogView dv = mgMapActivity.findViewById(R.id.dialog_parent);
        int cnt = 0;
        while (isRunning() && (dv.getChildCount() == 0)){
            mgLog.d("download not finished "+ cnt++);
            WaitUtil.doWait(TestControl.class, 1000);
        }
        // confirm map download result report
        animateTo(testControl.getViewClickPos("bgJobGroupResult_Download_map_btPositive"), 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 1000);

        // now confirm HintMapLayerAssignment
        animateTo(testControl.getViewClickPos("HintMapLayerAssignment_btPositive"), 600);
        doClick();

        testControl.setCursorVisibility(true);
        WaitUtil.doWait(TestControl.class, 1000);

        // after this hint the map layer selection screen is opened - now select layer2
        animateTo(testControl.getPreferenceCenter(R.string.Layers_pref_chooseMap2_key), 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 1000);

        // simulate the selection of entry "MAPSFORGE: Berlin_oam.osm.map" (close dialog and set property to this value)
        testControl.handleSettingsDialog(R.string.Layers_pref_chooseMap2_key,"MAPSFORGE: Berlin_oam.osm.map");
        WaitUtil.doWait(TestControl.class, 2000);

        // simulate click on Android back button (mo cursor to this position, but then finish Settings activity, since we cannot click outside of the app)
        animateTo( getPercentPosition(20,96), 1000);
        testControl.getActivity(SettingsActivity.class).finish();

        WaitUtil.doWait(TestControl.class, 5000);
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("onClick HintInitialMapDownload_btPositive");
        addRegex("onClick group_task");
        addRegex("onClick download");
        addRegex("onPreferenceClick https://www.openandromaps.org/downloads/deutschland");
        addRegex("showHint key=hint_initial_map_download2_key");
        addRegex("onClick HintInitialMapDownload2_btPositive");
        addRegex("Intent . act=android.intent.action.VIEW dat=mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip");
        addRegex("onClick bgJobGroupConfirm_Download_map_btPositive"); // confirm download
        addRegex("BgJobGroup.jobFinished.* successCounter=1  errorCounter=0  jobCounter=1");
        addRegex("onClick bgJobGroupResult_Download_map_btPositive"); // confirm download finished
        addRegex("showHint key=hint_map_layer_assignment_key");
        addRegex("onClick HintMapLayerAssignment_btPositive");
        addRegex("MapLayerListPreference.onClick.* key=SelectMap2 value=none");
        addRegex("recreate MGMapActivity due to key=SelectMap2 value=MAPSFORGE: Berlin_oam.osm.map");
        addRegex(getName()+" stop");
    }

}
