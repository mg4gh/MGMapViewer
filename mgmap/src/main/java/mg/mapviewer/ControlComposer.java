package mg.mapviewer;

import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

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
import mg.mapviewer.features.marker.MSMarker;
import mg.mapviewer.features.position.MSPosition;
import mg.mapviewer.features.remainings.MSRemainings;
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.routing.MSRoutingHintService;
import mg.mapviewer.features.rtl.MSRecordingTrackLog;
import mg.mapviewer.features.search.MSSearch;
import mg.mapviewer.features.time.MSTime;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;

public class ControlComposer {

    MGPref<Integer> prefQcs = MGPref.get(R.string.MSControl_qc_selector, 0);

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

        ViewGroup qcsParent = activity.findViewById(R.id.base);
        ViewGroup[] qcss = new ViewGroup[8];
        ArrayList<Observer> gos = new ArrayList<>();
        for (int idx=0; idx<qcss.length; idx++){
            qcss[idx] = ControlView.createRow(coView.getContext());
            final int iidx = idx;
            gos.add(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    prefQcs.setValue(iidx);
                }
            });
        }
        application.getMS(MSControl.class).initQcss(qcss);

        ViewGroup qcs = qcss[0];
        createQC(application,MSControl.class,qcss[0],"group_task",gos.get(1));
        createQC(application,MSSearch.class,qcss[0],"group_search",gos.get(2));
        ControlView.createQuickControlETV(qcss[0]).setPrAction(MGPref.anonymous(false))
                .setData(MGPref.bool(R.string.MSMarker_qc_EditMarkerTarck),MGPref.bool(R.string.MSRouting_qc_RoutingHint),
                R.drawable.group_marker1, R.drawable.group_marker2, R.drawable.group_marker3, R.drawable.group_marker4).addActionObserver(gos.get(3));
        createQC(application,MSBB.class,qcss[0],"group_bbox",gos.get(4));
        createQC(application,MSPosition.class,qcss[0],"group_record",gos.get(5));
        ControlView.createQuickControlETV(qcss[0]).setPrAction(MGPref.anonymous(false)).setData(R.drawable.show_hide).addActionObserver(gos.get(6));
        createQC(application,MSControl.class,qcss[0],"group_multi",gos.get(7));

        createQC(application,MSControl.class,qcss[1],"help_task",gos.get(1));
        createQC(application,MSControl.class,qcss[1],"settings",gos.get(0));
        createQC(application,MSControl.class,qcss[1],"fuSettings",gos.get(0));
        createQC(application,MSControl.class,qcss[1],"statistic",gos.get(0));
        createQC(application,MSControl.class,qcss[1],"heightProfile",gos.get(0));
        createQC(application,MSControl.class,qcss[1],"download",gos.get(0));
        createQC(application,MSControl.class,qcss[1],"empty",gos.get(0));

        createQC(application,MSSearch.class,qcss[2],"help_search",gos.get(2));
        createQC(application,MSSearch.class,qcss[2],"search",gos.get(0));
        createQC(application,MSSearch.class,qcss[2],"searchRes",gos.get(0));
        createQC(application,MSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[2],"empty",gos.get(0));

        createQC(application,MSSearch.class,qcss[3],"help_marker",gos.get(3));
        createQC(application,MSControl.class,qcss[3],"empty",gos.get(0));
        createQC(application,MSMarker.class,qcss[3],"markerEdit",gos.get(0));
        createQC(application,MSRoutingHintService.class,qcss[3],"routingHint",gos.get(0));
        createQC(application,MSControl.class,qcss[3],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[3],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[3],"empty",gos.get(0));

        createQC(application,MSBB.class,qcss[4],"help_bb",gos.get(4));
        createQC(application,MSControl.class,qcss[4],"empty",gos.get(0));
        createQC(application,MSBB.class,qcss[4],"loadFromBB",gos.get(0));
        createQC(application,MSBB.class,qcss[4],"bbox_on",gos.get(0));
        createQC(application,MSBB.class,qcss[4],"TSLoadRemain",gos.get(0));
        createQC(application,MSBB.class,qcss[4],"TSLoadAll",gos.get(0));
        createQC(application,MSBB.class,qcss[4],"TSDeleteAll",gos.get(0));

        createQC(application,MSControl.class,qcss[5],"help_record",gos.get(5));
        createQC(application,MSControl.class,qcss[5],"empty",gos.get(0));
        createQC(application,MSPosition.class,qcss[5],"center",gos.get(0));
        createQC(application,MSPosition.class,qcss[5],"gps",gos.get(0));
        createQC(application,MSRecordingTrackLog.class,qcss[5],"track",gos.get(0));
        createQC(application,MSRecordingTrackLog.class,qcss[5],"segment",gos.get(0));
        createQC(application,MSControl.class,qcss[5],"empty",gos.get(0));

        createQC(application,MSControl.class,qcss[6],"help_hide",gos.get(6));
        createQC(application,MSAlpha.class,qcss[6],"alpha_layers",gos.get(0));
        createQC(application,MSAlpha.class,qcss[6],"alpha_tracks",gos.get(0));
        createQC(application,MSAvailableTrackLogs.class,qcss[6],"hide_stl",gos.get(0));
        createQC(application,MSAvailableTrackLogs.class,qcss[6],"hide_atl",gos.get(0));
        createQC(application,MSAvailableTrackLogs.class,qcss[6],"hide_all",gos.get(0));
        createQC(application,MSMarker.class,qcss[6],"hide_mtl",gos.get(0));

        createQC(application,MSControl.class,qcss[7],"help_extra",gos.get(7));
        createQC(application,MSControl.class,qcss[7],"exit",gos.get(0));
        createQC(application,MSControl.class,qcss[7],"empty",gos.get(0));
        createQC(application,MSControl.class,qcss[7],"fullscreen",gos.get(0));
        createQC(application,MSControl.class,qcss[7],"zoom_in",gos.get(7));
        createQC(application,MSControl.class,qcss[7],"zoom_out",gos.get(7));
        createQC(application,MSControl.class,qcss[7],"home",gos.get(0));
    }

    private ExtendedTextView createQC(MGMapApplication application, Class<? extends MGMicroService> clazz, ViewGroup viewGroup, String info, Observer grObserver){
        return application.getMS(clazz).initQuickControl(ControlView.createQuickControlETV(viewGroup), info).addActionObserver(grObserver);
    }



}
