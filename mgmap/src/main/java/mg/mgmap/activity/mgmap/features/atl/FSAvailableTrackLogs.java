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
package mg.mgmap.activity.mgmap.features.atl;

import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;

import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.model.TrackLogRefZoom;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;

public class FSAvailableTrackLogs extends FeatureService {

    private final Paint PAINT_STROKE_ATL = CC.getStrokePaint(R.color.CC_GREEN, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL = CC.getStrokePaint(R.color.CC_BLUE, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL2 = CC.getFillPaint(R.color.CC_BLUE);
    private final Paint PAINT_STROKE_STL_GL = CC.getStrokePaint(R.color.CC_GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private final Pref<Boolean> prefStlGl = getPref(R.string.FSATL_pref_stlGl, false);
    private final Pref<Float> prefAlphaStl = getPref(R.string.FSATL_pref_alphaSTL, 1.0f);
    private final Pref<Float> prefAlphaAtl = getPref(R.string.FSATL_pref_alphaATL, 1.0f);
    private final Pref<Boolean> prefStlVisibility = getPref(R.string.FSATL_pref_STL_visibility, false);
    private final Pref<Boolean> prefAtlVisibility = getPref(R.string.FSATL_pref_ATL_visibility, false);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);

    private final Pref<Boolean> triggerHideStl = new Pref<>(false);
    private final Pref<Boolean> triggerHideAtl = new Pref<>(false);
    private final Pref<Boolean> triggerHideAll = getPref(R.string.FSATL_pref_hideAll, false);
    private final Pref<Boolean> prefHideAllEnabled = new Pref<>(false);

    private ViewGroup dashboardStl = null;
    private ViewGroup dashboardStls = null;

    public FSAvailableTrackLogs(MGMapActivity mmActivity) {
        super(mmActivity);
        triggerHideStl.addObserver((e) -> getApplication().availableTrackLogsObservable.removeSelected());
        triggerHideAtl.addObserver((e) -> getApplication().availableTrackLogsObservable.removeUnselected());
        triggerHideAll.addObserver((e) -> getApplication().availableTrackLogsObservable.removeAll());
        Observer hideAllEnabledObserver = (e) -> prefHideAllEnabled.setValue( prefStlVisibility.getValue() || prefAtlVisibility.getValue() || prefMtlVisibility.getValue() );
        prefStlVisibility.addObserver(hideAllEnabledObserver);
        prefAtlVisibility.addObserver(hideAllEnabledObserver);
        prefMtlVisibility.addObserver(hideAllEnabledObserver);

        getApplication().availableTrackLogsObservable.addObserver(refreshObserver);
        prefAlphaStl.addObserver(refreshObserver);
        prefAlphaAtl.addObserver(refreshObserver);
        prefStlGl.addObserver(refreshObserver);
        ttRefreshTime = 30;
    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.CC_WHITE, R.color.CC_BLUE100_A100);
        if ("stl".equals(info)) {
            dashboardStl = dvg;
        }
        if ("stls".equals(info)) {
            dashboardStls = dvg;
        }
        return dvg;
    }

    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("stl".equals(info)) {
            lsl.initPrefData(prefStlVisibility, prefAlphaStl, CC.getColor(R.color.CC_BLUE), "SelectedTrackLog");
        }
        if ("atl".equals(info)) {
            lsl.initPrefData(prefAtlVisibility, prefAlphaAtl, CC.getColor(R.color.CC_GREEN), "AvailableTrackLog");
        }
        return lsl;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("hide_stl".equals(info)){
            etv.setData(R.drawable.hide_stl);
            etv.setPrAction(triggerHideStl);
            etv.setDisabledData(prefStlVisibility, R.drawable.hide_stl_dis);
            etv.setHelp(r(R.string.FSATL_qcHideStl_Help));
        } else if ("hide_atl".equals(info)){
            etv.setData(R.drawable.hide_atl);
            etv.setPrAction(triggerHideAtl);
            etv.setDisabledData(prefAtlVisibility,R.drawable.hide_atl_dis);
            etv.setHelp(r(R.string.FSATL_qcHideAtl_Help));
        } else if ("hide_all".equals(info)){
            etv.setData(R.drawable.hide_all);
            etv.setPrAction(triggerHideAll);
            etv.setDisabledData(prefHideAllEnabled,R.drawable.hide_all_dis);
            etv.setHelp(r(R.string.FSATL_qcHideAll_Help));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        showAvailableTrackLogs();
    }

    private void showAvailableTrackLogs(){
        hideAvailableTrackLogs();

        MGMapApplication.AvailableTrackLogsObservable available = getApplication().availableTrackLogsObservable;
        boolean bAtlAlphaVisibility = false;
        for (TrackLog trackLog: new ArrayList<>(available.getAvailableTrackLogs())){
            if ((trackLog != available.getSelectedTrackLogRef().getTrackLog()) &&
                    (trackLog != getApplication().recordingTrackLogObservable.getTrackLog()) &&
                    (trackLog != getApplication().markerTrackLogObservable.getTrackLog()) &&
                    (trackLog != getApplication().routeTrackLogObservable.getTrackLog())  ) {
                showTrack(trackLog, CC.getAlphaClone(PAINT_STROKE_ATL, prefAlphaAtl.getValue()), false, -1);
                bAtlAlphaVisibility = true;
            }
        }
        prefAtlVisibility.setValue(bAtlAlphaVisibility);
        // handle selected track last, to get it on top
        TrackLog trackLog = available.getSelectedTrackLogRef().getTrackLog();
        if (trackLog!= null){
            assert (available.getAvailableTrackLogs().contains(trackLog));
            showTrack(trackLog,prefStlGl,PAINT_STROKE_STL_GL,PAINT_STROKE_STL, prefAlphaStl.getValue(), -1);
//            if ((getPref(R.string.FSBeeline_pref_ZoomLevel, 15).getValue() >= 12) && getPref(R.string.preferences_display_show_km_key, false).getValue()){
            if (getPref(R.string.preferences_display_show_km_key, false).getValue()){
                showTrack(trackLog,CC.getAlphaCloneFill(PAINT_STROKE_STL2, prefAlphaStl.getValue()),false, 3, true, true);
            }
            getControlView().setDashboardValue(true, dashboardStl ,trackLog.getTrackStatistic());
            int segIdx = available.getSelectedTrackLogRef().getSegmentIdx();
            getControlView().setDashboardValue(trackLog.getNumberOfSegments()>1, dashboardStls,(segIdx>=0)?trackLog.getTrackLogSegment(segIdx).getStatistic():null);
            TrackLogRef ref = available.getSelectedTrackLogRef();
            if (ref instanceof TrackLogRefZoom trackLogRefZoom) {
                if (trackLogRefZoom.isZoomForBB()){
                    trackLogRefZoom.setZoomForBB(false); // should be applied only once
                    getMapViewUtility().zoomForBoundingBox(trackLog.getBBox());
                }
            }
        }
        prefStlVisibility.setValue((trackLog != null) && (trackLog.getTrackStatistic().getNumPoints() >=1));
        prefStlVisibility.onChange();
    }

    private void hideAvailableTrackLogs(){
        unregisterAll();
        getControlView().setDashboardValue(false,dashboardStl,null);
        getControlView().setDashboardValue(false,dashboardStls,null);
    }
}
