package mg.mapviewer;

import android.view.ViewGroup;

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
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.routing.RoutingHintService;
import mg.mapviewer.features.rtl.MSRecordingTrackLog;
import mg.mapviewer.features.search.MSSearch;

public class ControlComposer {

    void compose(MGMapApplication application, MGMapActivity activity, ControlView coView) {
        composeMenu(application, activity, coView);
    }

    void composeMenu(MGMapApplication application, MGMapActivity activity, ControlView coView){
        coView.registerControl(activity.findViewById(R.id.bt_st01), new SettingsControl());
        coView.registerControl(activity.findViewById(R.id.bt_st02), new ThemeSettingsControl());
        coView.registerControl(activity.findViewById(R.id.bt_st03), new TrackStatisticControl());
        coView.registerControl(activity.findViewById(R.id.bt_st04), new HeightProfileControl());
        coView.registerControl(activity.findViewById(R.id.bt_st05), new CenterControl());
        coView.registerControl(activity.findViewById(R.id.bt_st06), new GpsControl());

        coView.registerMenuControl(activity.findViewById(R.id.bt_en01), coView.rstring(R.string.btRecordTrack), (ViewGroup) activity.findViewById(R.id.menu_en01_sub), application.getMS(MSRecordingTrackLog.class).getMenuTrackControls());
        coView.registerMenuControl(activity.findViewById(R.id.bt_en02), coView.rstring(R.string.btBB), (ViewGroup) activity.findViewById(R.id.menu_en02_sub), application.getMS(MSBB.class).getMenuBBControls());
        coView.registerMenuControl(activity.findViewById(R.id.bt_en03), coView.rstring(R.string.btLoadTrack),   (ViewGroup) activity.findViewById(R.id.menu_en03_sub), application.getMS(MSAvailableTrackLogs.class).getMenuLoadControls());
        coView.registerMenuControl(activity.findViewById(R.id.bt_en04), coView.rstring(R.string.btHideTrack),   (ViewGroup) activity.findViewById(R.id.menu_en04_sub), application.getMS(MSAvailableTrackLogs.class).getMenuHideControls());
        coView.registerMenuControl(activity.findViewById(R.id.bt_en05), coView.rstring(R.string.btMarkerTrack), (ViewGroup) activity.findViewById(R.id.menu_en05_sub), application.getMS(MSMarker.class).getMenuMarkerControls());
        coView.registerMenuControl(activity.findViewById(R.id.bt_en06), coView.rstring(R.string.btRoute),        (ViewGroup) activity.findViewById(R.id.menu_en06_sub), application.getMS(MSRouting.class).getMenuRouteControls());
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
}
