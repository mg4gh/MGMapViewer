/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;

import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.atl.FSAvailableTrackLogs;
import mg.mgmap.activity.mgmap.features.control.FSControl;
import mg.mgmap.activity.mgmap.features.beeline.FSBeeline;
import mg.mgmap.activity.mgmap.features.marker.FSMarker;
import mg.mgmap.activity.mgmap.features.routing.FSRouting;
import mg.mgmap.activity.mgmap.features.alpha.FSAlpha;
import mg.mgmap.activity.mgmap.features.bb.FSBB;
import mg.mgmap.activity.mgmap.features.position.FSPosition;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.activity.mgmap.features.rtl.FSRecordingTrackLog;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.time.FSTime;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;

public class ControlComposer {


    void composeDashboard(MGMapActivity activity, ControlView coView){
        activity.getFS(FSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtl");
        activity.getFS(FSRecordingTrackLog.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "rtls");
        activity.getFS(FSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stl");
        activity.getFS(FSAvailableTrackLogs.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "stls");
        activity.getFS(FSRouting.class).initDashboard( composeDashboardEntry(coView, coView.createDashboardEntry() ), "route");
    }

    ViewGroup composeDashboardEntry(ControlView coView, ViewGroup dashboardEntry){
        coView.createDashboardETV(dashboardEntry, 10).setFormat(Formatter.FormatType.FORMAT_STRING);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setData(R.drawable.length);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.gain);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.loss);
        coView.createDashboardETV(dashboardEntry, 20).setFormat(Formatter.FormatType.FORMAT_DURATION).setData(R.drawable.duration);
        float totalWeight = 0;
        for (int i=0; i<dashboardEntry.getChildCount(); i++){
            totalWeight += ((TableRow.LayoutParams)dashboardEntry.getChildAt(i).getLayoutParams()).weight;
        }
        int totalWidth = coView.getActivity().getResources().getDisplayMetrics().widthPixels;
        for (int i=0; i<dashboardEntry.getChildCount(); i++){
            TableRow.LayoutParams params = (TableRow.LayoutParams)dashboardEntry.getChildAt(i).getLayoutParams();
            params.width = (int)( params.weight*totalWidth / totalWeight) - params.leftMargin - params.rightMargin;
            dashboardEntry.getChildAt(i).setLayoutParams(params);
        }
        coView.dashboardEntries.add(dashboardEntry);
        return dashboardEntry;
    }

    void composeRoutingProfileButtons(MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.routingProfiles);
        for (RoutingProfile routingProfile : FSRouting.getDefinedRoutingProfiles()){
            activity.getFS(FSRouting.class).initRoutingProfile(coView.createRoutingProfileETV(parent), routingProfile).setIdOnly(View.generateViewId());
        }
        parent.removeAllViews();
        parent.setVisibility(View.INVISIBLE);
    }

    private int getSliderId(int idx){
        return switch (idx){
            case 0 -> R.id.slider_map1;
            case 1 -> R.id.slider_map2;
            case 2 -> R.id.slider_map3;
            case 3 -> R.id.slider_map4;
            case 4 -> R.id.slider_map5;
            default -> throw new RuntimeException("Map idx out of bound [0..4]: "+idx);
        };
    }

    void composeAlphaSlider(MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.bars);
        for (int idx=0; idx<MGMapLayerFactory.NUM_MAP_LAYERS; idx++){
            coView.createLabeledSlider(parent).setId(getSliderId(idx));
        }
        parent.setVisibility(View.INVISIBLE);
    }
    void configureAlphaSlider(MGMapActivity activity, ControlView coView) {
        MGMapLayerFactory mapLayerFactory = activity.getMapLayerFactory();
        ArrayList<LabeledSlider> sliders = coView.labeledSliders;
        if (sliders != null){
            for (int idx=0; idx<MGMapLayerFactory.NUM_MAP_LAYERS; idx++) {
                LabeledSlider labeledSlider = sliders.get(idx);
                final String key = mapLayerFactory.getMapLayerKey(idx);
                if (mapLayerFactory.hasAlpha(idx)){
                    labeledSlider.initPrefData(null, activity.getPrefCache().get("alpha_" + key, 1.0f), null, key);
                } else {
                    labeledSlider.initPrefData(null, null, null, null);
                }
            }
            coView.reworkLabeledSliderVisibility();
        }
    }

    void composeAlphaSlider2(MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.bars2);
        activity.getFS(FSRecordingTrackLog.class).initLabeledSlider(coView.createLabeledSlider2(parent), "rtl").setId(R.id.slider_rtl);
        activity.getFS(FSMarker.class).initLabeledSlider(coView.createLabeledSlider2(parent), "mtl").setId(R.id.slider_mtl);
        activity.getFS(FSRouting.class).initLabeledSlider(coView.createLabeledSlider2(parent), "rotl").setId(R.id.slider_rotl);
        activity.getFS(FSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider2(parent), "stl").setId(R.id.slider_stl);
        activity.getFS(FSAvailableTrackLogs.class).initLabeledSlider(coView.createLabeledSlider2(parent), "atl").setId(R.id.slider_atl);
        coView.registerSliderVisibilityObserver2();
        parent.setVisibility(View.INVISIBLE);
    }

    void composeStatusLine(MGMapActivity activity, ControlView coView){
        ViewGroup parent = activity.findViewById(R.id.tr_states);
        activity.getFS(FSBeeline.class).initStatusLine(coView.createStatusLineETV(parent, 20), "center");
        activity.getFS(FSBeeline.class).initStatusLine(coView.createStatusLineETV(parent, 10), "zoom");
        activity.getFS(FSTime.class).initStatusLine(coView.createStatusLineETV(parent, 15), "time");
        activity.getFS(FSTime.class).initStatusLine(coView.createStatusLineETV(parent, 20), "job");
        activity.getFS(FSPosition.class).initStatusLine(coView.createStatusLineETV(parent, 20), "height");
        activity.getFS(FSTime.class).initStatusLine(coView.createStatusLineETV(parent, 15), "bat");
    }

    void composeQuickControls(MGMapActivity activity, ControlView coView) {
        Pref<Boolean> prefEditMarkerTrack = activity.getPrefCache().get(R.string.FSMarker_qc_EditMarkerTrack, false);
        Pref<Boolean> prefRoutingHints = activity.getPrefCache().get(R.string.FSRouting_qc_RoutingHint, false);
        Pref<Integer> prefQcs = activity.getPrefCache().get(R.string.FSControl_qc_selector, 0);


        ViewGroup[] qcss = new ViewGroup[8]; // menu group + 7 menu item groups

        ArrayList<Observer> gos = new ArrayList<>();
        for (int idx=0; idx<qcss.length; idx++){
            qcss[idx] = VUtil.createQCRow(coView.getContext());
            final int iidx = idx;
            // if menu qc is pressed, then inflate - except this is already inflated, then deflate
            gos.add((e) -> prefQcs.setValue( (prefQcs.getValue()== iidx)?0:iidx ));
            // menus are plced in base3 while menu items are placed in base2
            ViewGroup qcsParent = activity.findViewById((idx==0)?R.id.base3:R.id.base2);
            qcsParent.addView(qcss[idx]);
            qcss[idx].setVisibility(View.INVISIBLE);
        }
        activity.getFS(FSControl.class).initQcss(qcss);

        createQC(activity, FSControl.class,qcss[0],"group_task",gos.get(1)).setId(R.id.menu_task);
        createQC(activity, FSSearch.class,qcss[0],"group_search",gos.get(2)).setId(R.id.menu_search);
        VUtil.createQuickControlETV(qcss[0],false).setPrAction(new Pref<>(false))
                .setData(prefEditMarkerTrack,prefRoutingHints,R.drawable.group_marker1, R.drawable.group_marker2, R.drawable.group_marker3, R.drawable.group_marker4)
                .setName("group_marker").addActionObserver(gos.get(3)).setId(R.id.menu_marker);
        createQC(activity, FSBB.class,qcss[0],"group_bbox",gos.get(4)).setId(R.id.menu_bb);
        createQC(activity, FSPosition.class,qcss[0],"group_record",gos.get(5)).setId(R.id.menu_gps);
        VUtil.createQuickControlETV(qcss[0],false).setPrAction(new Pref<>(false)).setData(R.drawable.group_hide)
                .setName("group_showHide").addActionObserver(gos.get(6)).setId(R.id.menu_show_hide);
        createQC(activity, FSControl.class,qcss[0],"group_multi",gos.get(7)).setId(R.id.menu_multi);

        createQC(activity, FSControl.class,qcss[1],"help").setId(R.id.mi_task_help);
        createQC(activity, FSControl.class,qcss[1],"settings",gos.get(0)).setId(R.id.mi_settings);
        createQC(activity, FSControl.class,qcss[1],"fileMgr",gos.get(0)).setId(R.id.mi_fileMgr);
        createQC(activity, FSControl.class,qcss[1],"statistic",gos.get(0)).setId(R.id.mi_statistic);
        createQC(activity, FSControl.class,qcss[1],"heightProfile",gos.get(0)).setId(R.id.mi_height_profile);
        createQC(activity, FSControl.class,qcss[1],"download",gos.get(0)).setId(R.id.mi_download);
        createQC(activity, FSControl.class,qcss[1],"themes",gos.get(0)).setId(R.id.mi_themes);

        createQC(activity, FSControl.class,qcss[2],"help").setId(R.id.mi_search_help);
        createQC(activity, FSSearch.class,qcss[2],"search",gos.get(0)).setId(R.id.mi_search);
        createQC(activity, FSSearch.class,qcss[2],"searchRes",gos.get(0)).setId(R.id.mi_search_res);
        createQC(activity, FSSearch.class,qcss[2],"posBasedSearch",gos.get(0)).setId(R.id.mi_search_posBased);
        createQC(activity, FSControl.class,qcss[2],"empty",gos.get(0)).setId(R.id.mi_search_empty2);
        createQC(activity, FSControl.class,qcss[2],"empty",gos.get(0)).setId(R.id.mi_search_empty3);
        createQC(activity, FSSearch.class,qcss[2],"selpro",gos.get(0)).setId(R.id.mi_search_empty4);

        createQC(activity, FSControl.class,qcss[3],"help").setId(R.id.mi_marker_help);
        createQC(activity, FSRouting.class,qcss[3],"routingSave",gos.get(0)).setId(R.id.mi_routing_save);
        createQC(activity, FSMarker.class,qcss[3],"markerEdit",gos.get(0)).setId(R.id.mi_marker_edit);
        createQC(activity, FSRouting.class,qcss[3],"routingHint",gos.get(0)).setId(R.id.mi_routing_hint);
        createQC(activity, FSMarker.class,qcss[3],"reverse",gos.get(0)).setId(R.id.mi_marker_reverse);
        createQC(activity, FSRouting.class,qcss[3],"matching",gos.get(0)).setId(R.id.mi_map_mathching);
        createQC(activity, FSControl.class,qcss[3],"empty",gos.get(0)).setId(R.id.mi_marker_empty3);

        createQC(activity, FSControl.class,qcss[4],"help").setId(R.id.mi_bb_help);
        createQC(activity, FSControl.class,qcss[4],"empty",gos.get(0)).setId(R.id.mi_bb_empty1);
        createQC(activity, FSBB.class,qcss[4],"loadFromBB",gos.get(0)).setId(R.id.mi_load_from_bb);
        createQC(activity, FSBB.class,qcss[4],"bbox_on",gos.get(0)).setId(R.id.mi_bbox);
        createQC(activity, FSBB.class,qcss[4],"TSLoadRemain",gos.get(0)).setId(R.id.mi_load_remain);
        createQC(activity, FSBB.class,qcss[4],"TSLoadAll",gos.get(0)).setId(R.id.mi_load_all);
        createQC(activity, FSBB.class,qcss[4],"TSDeleteAll",gos.get(0)).setId(R.id.mi_delete_all);

        createQC(activity, FSControl.class,qcss[5],"help").setId(R.id.mi_gps_help);
        createQC(activity, FSControl.class,qcss[5],"empty",gos.get(0)).setId(R.id.mi_gps_empty1);
        createQC(activity, FSPosition.class,qcss[5],"center",gos.get(0)).setId(R.id.mi_gps_center);
        createQC(activity, FSPosition.class,qcss[5],"gps",gos.get(0)).setId(R.id.mi_gps_toggle);
        createQC(activity, FSRecordingTrackLog.class,qcss[5],"track",gos.get(0)).setId(R.id.mi_record_track);
        createQC(activity, FSRecordingTrackLog.class,qcss[5],"segment",gos.get(0)).setId(R.id.mi_record_segment);
        createQC(activity, FSControl.class,qcss[5],"empty",gos.get(0)).setId(R.id.mi_gps_empty2);

        createQC(activity, FSControl.class,qcss[6],"help").setId(R.id.mi_sh_help);
        createQC(activity, FSAlpha.class,qcss[6],"alpha_layers",gos.get(0)).setId(R.id.mi_alpha_layers);
        createQC(activity, FSAlpha.class,qcss[6],"alpha_tracks",gos.get(0)).setId(R.id.mi_alpha_tracks);
        createQC(activity, FSAvailableTrackLogs.class,qcss[6],"hide_stl",gos.get(0)).setId(R.id.mi_hide_stl);
        createQC(activity, FSAvailableTrackLogs.class,qcss[6],"hide_atl",gos.get(0)).setId(R.id.mi_hide_atl);
        createQC(activity, FSAvailableTrackLogs.class,qcss[6],"hide_all",gos.get(0)).setId(R.id.mi_hide_all);
        createQC(activity, FSMarker.class,qcss[6],"hide_mtl",gos.get(0)).setId(R.id.mi_hide_mtl);

        createQC(activity, FSControl.class,qcss[7],"help").setId(R.id.mi_help_multi);
        createQC(activity, FSControl.class,qcss[7],"exit",gos.get(0)).setId(R.id.mi_exit);
        createQC(activity, FSControl.class,qcss[7],"empty",gos.get(0)).setId(R.id.mi_multi_empty1);
        createQC(activity, FSControl.class,qcss[7],"fullscreen",gos.get(0)).setId(R.id.mi_fullscreen);
        createQC(activity, FSControl.class,qcss[7],"zoom_in").setId(R.id.mi_zoom_in);
        createQC(activity, FSControl.class,qcss[7],"zoom_out").setId(R.id.mi_zoom_out);
        createQC(activity, FSControl.class,qcss[7],"home",gos.get(0)).setId(R.id.mi_home);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private ExtendedTextView createQC(MGMapActivity activity, Class<? extends FeatureService> clazz, ViewGroup viewGroup, String info){
        return createQC(activity,clazz,viewGroup,info,null);
    }
    private ExtendedTextView createQC(MGMapActivity activity, Class<? extends FeatureService> clazz, ViewGroup viewGroup, String info, Observer grObserver){
        return activity.getFS(clazz).initQuickControl(VUtil.createQuickControlETV(viewGroup,false), info).addActionObserver(grObserver);
    }

    public void composeHelpControls(MGMapActivity activity, ControlView coView) {
        LinearLayout help = activity.findViewById(R.id.help);
        LinearLayout help1 = coView.createHelpPanel(help, Gravity.CENTER, 0);

        activity.getFS(FSControl.class).initHelpControl(coView.createHelpText1(help1), "help1").setId(R.id.help1_close);
        LinearLayout help2 = coView.createHelpPanel(help, Gravity.START, -90);
        for (int i = 0; i < 7; i++) {
            @SuppressLint("DiscouragedApi") int id = activity.getResources().getIdentifier("help2_text"+i, "id", activity.getPackageName());
            activity.getFS(FSControl.class).initHelpControl(coView.createHelpText2(help2), "help2").setId(id);
        }
        LinearLayout help3 = coView.createHelpPanel(help, Gravity.START, 0);
        activity.getFS(FSControl.class).initHelpControl(coView.createHelpText3(help3), "help3").setId(R.id.help3_empty);
        help.setVisibility(View.INVISIBLE);
    }
}
