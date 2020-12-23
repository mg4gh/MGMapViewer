package mg.mapviewer;

import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import mg.mapviewer.control.MSControl;
import mg.mapviewer.features.beeline.MSBeeline;
import mg.mapviewer.features.position.CenterControl;
import mg.mapviewer.features.position.GpsControl;
import mg.mapviewer.control.HeightProfileControl;
import mg.mapviewer.control.SettingsControl;
import mg.mapviewer.control.ThemeSettingsControl;
import mg.mapviewer.features.statistic.TrackStatisticControl;
import mg.mapviewer.features.alpha.MSAlpha;
import mg.mapviewer.features.atl.MSAvailableTrackLogs;
import mg.mapviewer.features.bb.MSBB;
import mg.mapviewer.features.fullscreen.MSFullscreen;
import mg.mapviewer.features.marker.MSMarker;
import mg.mapviewer.features.position.MSPosition;
import mg.mapviewer.features.remainings.MSRemainings;
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.routing.RoutingHintService;
import mg.mapviewer.features.rtl.MSRecordingTrackLog;
import mg.mapviewer.features.search.MSSearch;
import mg.mapviewer.features.time.MSTime;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.PrefTextView;

public class ControlComposer {

    void composeDashboard(MGMapApplication application, MGMapActivity activity, ControlView coView){
        application.getMS(MSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtl");
        application.getMS(MSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtls");
        application.getMS(MSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stl");
        application.getMS(MSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stls");
        application.getMS(MSRouting.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "route");
    }

    ViewGroup composeDashboardEntry(ControlView coView, ViewGroup dashboardEntry){
        coView.createDashboardPTV(dashboardEntry, 10).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(null, null);
        coView.createDashboardPTV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setPrefData(null, new int[]{R.drawable.length});
        coView.createDashboardPTV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.gain});
        coView.createDashboardPTV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.loss});
        coView.createDashboardPTV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DURATION).setPrefData(null, new int[]{R.drawable.duration});
        return dashboardEntry;
    }

    void composeMenu(MGMapApplication application, MGMapActivity activity, ControlView coView){
        View alignHelper = null;
        ConstraintLayout parent = activity.findViewById(R.id.menuBase);
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent, false, 0), new SettingsControl());
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent, false, 0), new ThemeSettingsControl());
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent,false, 0), new TrackStatisticControl());
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent,false, 0), new HeightProfileControl());
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent,false, 0), new CenterControl());
        alignHelper = coView.registerMenuControl(coView.createMenuButton(parent, alignHelper, parent,false, 0), new GpsControl());

        alignHelper = null;
        Control[] controls = application.getMS(MSRecordingTrackLog.class).getMenuTrackControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length) , coView.rstring(R.string.btRecordTrack),controls);
        controls = application.getMS(MSBB.class).getMenuBBControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btBB), controls);
        controls = application.getMS(MSAvailableTrackLogs.class).getMenuLoadControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btLoadTrack), controls);
        controls = application.getMS(MSAvailableTrackLogs.class).getMenuHideControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btHideTrack), controls);
        controls = application.getMS(MSMarker.class).getMenuMarkerControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btMarkerTrack), controls);
        controls = application.getMS(MSRouting.class).getMenuRouteControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btRoute), controls);
    }

    void composeAlphaSlider(MGMapApplication application, MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.bars);
        for (String prefKey : application.getMapLayerKeys()) {
            final String key = activity.sharedPreferences.getString(prefKey, "");
            if (MGMapLayerFactory.hasAlpha(key)){
                MGPref<Boolean> visibility = new MGPref<Boolean>("alpha_" + key+"_visibility", true, false);
                coView.createLabeledSlider(parent).initPrefData(visibility, MGPref.get("alpha_" + key, 1.0f), null, key);
            }
        }
        parent.setVisibility(View.INVISIBLE);
    }

    void composeAlphaSlider2(MGMapApplication application, MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.bars2);
        application.getMS(MSRecordingTrackLog.class).initLabeledSlider(coView.createLabeledSlider(parent), "rtl");
        application.getMS(MSMarker.class).initLabeledSlider(coView.createLabeledSlider(parent), "mtl");
        application.getMS(MSRouting.class).initLabeledSlider(coView.createLabeledSlider(parent), "rotl");
        application.getMS(MSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider(parent), "stl");
        application.getMS(MSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider(parent), "atl");
        parent.setVisibility(View.INVISIBLE);
    }

    void composeStatusLine(MGMapApplication application, MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.tr_states);
        application.getMS(MSBeeline.class).initStatusLine(coView.createStatusLinePTV(parent, 20), "center");
        application.getMS(MSBeeline.class).initStatusLine(coView.createStatusLinePTV(parent, 10), "zoom");
        application.getMS(MSTime.class).initStatusLine(coView.createStatusLinePTV(parent, 15), "time");
        application.getMS(MSPosition.class).initStatusLine(coView.createStatusLinePTV(parent, 20), "height");
        application.getMS(MSRemainings.class).initStatusLine(coView.createStatusLinePTV(parent, 20), "remain");
        application.getMS(MSTime.class).initStatusLine(coView.createStatusLinePTV(parent, 15), "bat");
    }

    void composeQuickControls(MGMapApplication application, MGMapActivity activity, ControlView coView) {
        ViewGroup qcs = activity.findViewById(R.id.tr_qc);
        PrefTextView ptvFullscreen = application.getMS(MSFullscreen.class).initQuickControl(coView.createQuickControlPTV(qcs,20), null);
        application.getMS(MSControl.class).initQuickControl(ptvFullscreen, "home2");
        application.getMS(MSControl.class).initQuickControl(ptvFullscreen, "qc2");
        application.getMS(MSAlpha.class).initQuickControl(coView.createQuickControlPTV(qcs,20), null);
        PrefTextView ptvEditMarker = application.getMS(MSMarker.class).initQuickControl(coView.createQuickControlPTV(qcs,20), null);
        RoutingHintService.getInstance().initQuickControl(ptvEditMarker, null);
        application.getMS(MSRouting.class).initQuickControl(ptvEditMarker, null);
        application.getMS(MSBB.class).initQuickControl(coView.createQuickControlPTV(qcs,20), null);
        application.getMS(MSSearch.class).initQuickControl(coView.createQuickControlPTV(qcs,20), null);
        activity.getMapViewUtility().initQuickControl(coView.createQuickControlPTV(qcs,20), "zoom_in");
        activity.getMapViewUtility().initQuickControl(coView.createQuickControlPTV(qcs,20), "zoom_out");


        ViewGroup qcs2 = activity.findViewById(R.id.tr_qc2);
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "settings");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "fuSettings");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "download");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "statistic");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "home");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "todo");
        application.getMS(MSControl.class).initQuickControl(coView.createQuickControlPTV(qcs2,20), "exit");

    }



}
