package mg.mapviewer;

import android.view.ViewGroup;

import mg.mapviewer.features.motion.MSBeeline;
import mg.mapviewer.features.position.CenterControl;
import mg.mapviewer.features.position.GpsControl;
import mg.mapviewer.control.HeightProfileControl;
import mg.mapviewer.control.SettingsControl;
import mg.mapviewer.control.ThemeSettingsControl;
import mg.mapviewer.control.TrackStatisticControl;
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
import mg.mapviewer.util.Formatter;

public class ControlComposer {

    void composeMenu(MGMapApplication application, MGMapActivity activity, ControlView coView){
        coView.registerMenuControl(activity.findViewById(R.id.bt_st01), new SettingsControl());
        coView.registerMenuControl(activity.findViewById(R.id.bt_st02), new ThemeSettingsControl());
        coView.registerMenuControl(activity.findViewById(R.id.bt_st03), new TrackStatisticControl());
        coView.registerMenuControl(activity.findViewById(R.id.bt_st04), new HeightProfileControl());
        coView.registerMenuControl(activity.findViewById(R.id.bt_st05), new CenterControl());
        coView.registerMenuControl(activity.findViewById(R.id.bt_st06), new GpsControl());

        coView.registerMenuControls(activity.findViewById(R.id.bt_en01), coView.rstring(R.string.btRecordTrack), (ViewGroup) activity.findViewById(R.id.menu_en01_sub), application.getMS(MSRecordingTrackLog.class).getMenuTrackControls());
        coView.registerMenuControls(activity.findViewById(R.id.bt_en02), coView.rstring(R.string.btBB), (ViewGroup) activity.findViewById(R.id.menu_en02_sub), application.getMS(MSBB.class).getMenuBBControls());
        coView.registerMenuControls(activity.findViewById(R.id.bt_en03), coView.rstring(R.string.btLoadTrack),   (ViewGroup) activity.findViewById(R.id.menu_en03_sub), application.getMS(MSAvailableTrackLogs.class).getMenuLoadControls());
        coView.registerMenuControls(activity.findViewById(R.id.bt_en04), coView.rstring(R.string.btHideTrack),   (ViewGroup) activity.findViewById(R.id.menu_en04_sub), application.getMS(MSAvailableTrackLogs.class).getMenuHideControls());
        coView.registerMenuControls(activity.findViewById(R.id.bt_en05), coView.rstring(R.string.btMarkerTrack), (ViewGroup) activity.findViewById(R.id.menu_en05_sub), application.getMS(MSMarker.class).getMenuMarkerControls());
        coView.registerMenuControls(activity.findViewById(R.id.bt_en06), coView.rstring(R.string.btRoute),        (ViewGroup) activity.findViewById(R.id.menu_en06_sub), application.getMS(MSRouting.class).getMenuRouteControls());
    }

    void composeQuickControls(MGMapApplication application, MGMapActivity activity, ControlView coView) {
        application.getMS(MSFullscreen.class).initQuickControl(activity.findViewById(R.id.ct1), null);
        application.getMS(MSAlpha.class).initQuickControl(activity.findViewById(R.id.ct2), null);
        application.getMS(MSMarker.class).initQuickControl(activity.findViewById(R.id.ct3), null);
        RoutingHintService.getInstance().initQuickControl(activity.findViewById(R.id.ct3), null);
        application.getMS(MSBB.class).initQuickControl(activity.findViewById(R.id.ct3a), null);
        application.getMS(MSSearch.class).initQuickControl(activity.findViewById(R.id.ct3b), null);
        activity.getMapViewUtility().initQuickControl(activity.findViewById(R.id.ct4), "zoom_in");
        activity.getMapViewUtility().initQuickControl(activity.findViewById(R.id.ct5), "zoom_out");
    }

    void composeStatusLine(MGMapApplication application, MGMapActivity activity, ControlView coView){
        application.getMS(MSBeeline.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl1), 20), "center");
        application.getMS(MSBeeline.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl2), 10), "zoom");
        application.getMS(MSTime.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl3), 15), "time");
        application.getMS(MSPosition.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl4), 20), "height");
        application.getMS(MSRemainings.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl5), 20), "remain");
        application.getMS(MSTime.class).initStatusLine(coView.setStatusLineLayout(activity.findViewById(R.id.tv_sl6), 15), "bat");
    }

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

}
