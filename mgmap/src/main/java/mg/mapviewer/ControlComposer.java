package mg.mapviewer;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Observer;

import mg.mapviewer.features.atl.FSAvailableTrackLogs;
import mg.mapviewer.features.control.FSControl;
import mg.mapviewer.features.beeline.FSBeeline;
import mg.mapviewer.features.marker.FSMarker;
import mg.mapviewer.features.position.CenterControl;
import mg.mapviewer.features.position.GpsControl;
import mg.mapviewer.features.control.HeightProfileControl;
import mg.mapviewer.features.control.SettingsControl;
import mg.mapviewer.features.control.ThemeSettingsControl;
import mg.mapviewer.features.routing.FSRouting;
import mg.mapviewer.features.statistic.TrackStatisticControl;
import mg.mapviewer.features.alpha.FSAlpha;
import mg.mapviewer.features.bb.FSBB;
import mg.mapviewer.features.position.FSPosition;
import mg.mapviewer.features.remainings.FSRemainings;
import mg.mapviewer.features.routing.FSRoutingHintService;
import mg.mapviewer.features.rtl.FSRecordingTrackLog;
import mg.mapviewer.features.search.FSSearch;
import mg.mapviewer.features.time.FSTime;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;

public class ControlComposer {

    MGPref<Integer> prefQcs = MGPref.get(R.string.FSControl_qc_selector, 0);

    void composeDashboard(MGMapApplication application, MGMapActivity activity, ControlView coView){
        application.getFS(FSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtl");
        application.getFS(FSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtls");
        application.getFS(FSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stl");
        application.getFS(FSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stls");
        application.getFS(FSRouting.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "route");
    }

    ViewGroup composeDashboardEntry(ControlView coView, ViewGroup dashboardEntry){
        coView.createDashboardETV(dashboardEntry, 10).setFormat(Formatter.FormatType.FORMAT_STRING);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setData(R.drawable.length);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.gain);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.loss);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DURATION).setData(R.drawable.duration);
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
        Control[] controls = application.getFS(FSRecordingTrackLog.class).getMenuTrackControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length) , coView.rstring(R.string.btRecordTrack),controls);
        controls = application.getFS(FSBB.class).getMenuBBControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btBB), controls);
        controls = application.getFS(FSAvailableTrackLogs.class).getMenuLoadControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btLoadTrack), controls);
        controls = application.getFS(FSAvailableTrackLogs.class).getMenuHideControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btHideTrack), controls);
        controls = application.getFS(FSMarker.class).getMenuMarkerControls();
        alignHelper = coView.registerMenuControls(coView.createMenuButton(parent, alignHelper, parent,true, controls.length), coView.rstring(R.string.btMarkerTrack), controls);
        controls = application.getFS(FSRouting.class).getMenuRouteControls();
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
        application.getFS(FSRecordingTrackLog.class).initLabeledSlider(coView.createLabeledSlider(parent), "rtl");
        application.getFS(FSMarker.class).initLabeledSlider(coView.createLabeledSlider(parent), "mtl");
        application.getFS(FSRouting.class).initLabeledSlider(coView.createLabeledSlider(parent), "rotl");
        application.getFS(FSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider(parent), "stl");
        application.getFS(FSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider(parent), "atl");
        parent.setVisibility(View.INVISIBLE);
    }

    void composeStatusLine(MGMapApplication application, MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.tr_states);
        application.getFS(FSBeeline.class).initStatusLine(coView.createStatusLineETV(parent, 20), "center");
        application.getFS(FSBeeline.class).initStatusLine(coView.createStatusLineETV(parent, 10), "zoom");
        application.getFS(FSTime.class).initStatusLine(coView.createStatusLineETV(parent, 15), "time");
        application.getFS(FSPosition.class).initStatusLine(coView.createStatusLineETV(parent, 20), "height");
        application.getFS(FSRemainings.class).initStatusLine(coView.createStatusLineETV(parent, 20), "remain");
        application.getFS(FSTime.class).initStatusLine(coView.createStatusLineETV(parent, 15), "bat");
    }

    void composeQuickControls(MGMapApplication application, MGMapActivity activity, ControlView coView) {

        ViewGroup[] qcss = new ViewGroup[8];
        ArrayList<Observer> gos = new ArrayList<>();
        for (int idx=0; idx<qcss.length; idx++){
            qcss[idx] = ControlView.createRow(coView.getContext());
            final int iidx = idx;
            gos.add((o, arg) -> prefQcs.setValue(iidx));
        }
        application.getFS(FSControl.class).initQcss(qcss);

        createQC(application, FSControl.class,qcss[0],"group_task",gos.get(1));
        createQC(application, FSSearch.class,qcss[0],"group_search",gos.get(2));
        ControlView.createQuickControlETV(qcss[0]).setPrAction(MGPref.anonymous(false))
                .setData(MGPref.bool(R.string.FSMarker_qc_EditMarkerTrack),MGPref.bool(R.string.FSRouting_qc_RoutingHint),
                R.drawable.group_marker1, R.drawable.group_marker2, R.drawable.group_marker3, R.drawable.group_marker4)
                .setName("group_marker").addActionObserver(gos.get(3));
        createQC(application, FSBB.class,qcss[0],"group_bbox",gos.get(4));
        createQC(application, FSPosition.class,qcss[0],"group_record",gos.get(5));
        ControlView.createQuickControlETV(qcss[0]).setPrAction(MGPref.anonymous(false)).setData(R.drawable.show_hide)
                .setName("group_showHide").addActionObserver(gos.get(6));
        createQC(application, FSControl.class,qcss[0],"group_multi",gos.get(7));

        createQC(application, FSControl.class,qcss[1],"help");
        createQC(application, FSControl.class,qcss[1],"settings",gos.get(0));
        createQC(application, FSControl.class,qcss[1],"fuSettings",gos.get(0));
        createQC(application, FSControl.class,qcss[1],"statistic",gos.get(0));
        createQC(application, FSControl.class,qcss[1],"heightProfile",gos.get(0));
        createQC(application, FSControl.class,qcss[1],"download",gos.get(0));
        createQC(application, FSControl.class,qcss[1],"themes",gos.get(0));

        createQC(application, FSControl.class,qcss[2],"help");
        createQC(application, FSSearch.class,qcss[2],"search",gos.get(0));
        createQC(application, FSSearch.class,qcss[2],"searchRes",gos.get(0));
        createQC(application, FSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application, FSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application, FSControl.class,qcss[2],"empty",gos.get(0));
        createQC(application, FSControl.class,qcss[2],"empty",gos.get(0));

        createQC(application, FSControl.class,qcss[3],"help");
        createQC(application, FSControl.class,qcss[3],"empty",gos.get(0));
        createQC(application, FSMarker.class,qcss[3],"markerEdit",gos.get(0));
        createQC(application, FSRoutingHintService.class,qcss[3],"routingHint",gos.get(0));
        createQC(application, FSControl.class,qcss[3],"empty",gos.get(0));
        createQC(application, FSRouting.class,qcss[3],"matching",gos.get(0));
        createQC(application, FSControl.class,qcss[3],"empty",gos.get(0));

        createQC(application, FSControl.class,qcss[4],"help");
        createQC(application, FSControl.class,qcss[4],"empty",gos.get(0));
        createQC(application, FSBB.class,qcss[4],"loadFromBB",gos.get(0));
        createQC(application, FSBB.class,qcss[4],"bbox_on",gos.get(0));
        createQC(application, FSBB.class,qcss[4],"TSLoadRemain",gos.get(0));
        createQC(application, FSBB.class,qcss[4],"TSLoadAll",gos.get(0));
        createQC(application, FSBB.class,qcss[4],"TSDeleteAll",gos.get(0));

        createQC(application, FSControl.class,qcss[5],"help");
        createQC(application, FSControl.class,qcss[5],"empty",gos.get(0));
        createQC(application, FSPosition.class,qcss[5],"center",gos.get(0));
        createQC(application, FSPosition.class,qcss[5],"gps",gos.get(0));
        createQC(application, FSRecordingTrackLog.class,qcss[5],"track",gos.get(0));
        createQC(application, FSRecordingTrackLog.class,qcss[5],"segment",gos.get(0));
        createQC(application, FSControl.class,qcss[5],"empty",gos.get(0));

        createQC(application, FSControl.class,qcss[6],"help");
        createQC(application, FSAlpha.class,qcss[6],"alpha_layers",gos.get(0));
        createQC(application, FSAlpha.class,qcss[6],"alpha_tracks",gos.get(0));
        createQC(application, FSAvailableTrackLogs.class,qcss[6],"hide_stl",gos.get(0));
        createQC(application, FSAvailableTrackLogs.class,qcss[6],"hide_atl",gos.get(0));
        createQC(application, FSAvailableTrackLogs.class,qcss[6],"hide_all",gos.get(0));
        createQC(application, FSMarker.class,qcss[6],"hide_mtl",gos.get(0));

        createQC(application, FSControl.class,qcss[7],"help");
        createQC(application, FSControl.class,qcss[7],"exit",gos.get(0));
        createQC(application, FSControl.class,qcss[7],"empty",gos.get(0));
        createQC(application, FSControl.class,qcss[7],"fullscreen",gos.get(0));
        createQC(application, FSControl.class,qcss[7],"zoom_in");
        createQC(application, FSControl.class,qcss[7],"zoom_out");
        createQC(application, FSControl.class,qcss[7],"home",gos.get(0));
    }

    private ExtendedTextView createQC(MGMapApplication application, Class<? extends FeatureService> clazz, ViewGroup viewGroup, String info){
        return createQC(application,clazz,viewGroup,info,null);
    }
    private ExtendedTextView createQC(MGMapApplication application, Class<? extends FeatureService> clazz, ViewGroup viewGroup, String info, Observer grObserver){
        return application.getFS(clazz).initQuickControl(ControlView.createQuickControlETV(viewGroup), info).addActionObserver(grObserver);
    }

    public void composeHelpControls(MGMapApplication application, MGMapActivity activity, ControlView coView) {
        LinearLayout help = activity.findViewById(R.id.help);
        LinearLayout help1 = coView.createHelpPanel(help, Gravity.CENTER, 0);
        application.getFS(FSControl.class).initHelpControl(coView.createHelpText1(help1), "help1");
        LinearLayout help2 = coView.createHelpPanel(help, Gravity.START, -90);
        for (int i = 0; i < 7; i++) {
            application.getFS(FSControl.class).initHelpControl(coView.createHelpText2(help2), "help2");
        }
        LinearLayout help3 = coView.createHelpPanel(help, Gravity.START, 0);
        application.getFS(FSControl.class).initHelpControl(coView.createHelpText3(help3), "help3");
        help.setVisibility(View.INVISIBLE);
    }
}
