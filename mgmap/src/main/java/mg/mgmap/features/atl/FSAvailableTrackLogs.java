/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mgmap.features.atl;

import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;
import mg.mgmap.R;

import java.util.Observer;

import mg.mgmap.model.BBox;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogRef;
import mg.mgmap.model.TrackLogRefZoom;
import mg.mgmap.util.Assert;
import mg.mgmap.util.CC;
import mg.mgmap.util.Control;
import mg.mgmap.util.MetaDataUtil;
import mg.mgmap.util.MGPref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.LabeledSlider;

public class FSAvailableTrackLogs extends FeatureService {

    private final Paint PAINT_STROKE_ATL = CC.getStrokePaint(R.color.GREEN, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL = CC.getStrokePaint(R.color.BLUE, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private final MGPref<Boolean> prefStlGl = MGPref.get(R.string.FSATL_pref_stlGl_key, true);
    private final MGPref<Float> prefAlphaStl = MGPref.get(R.string.FSATL_pref_alphaSTL, 1.0f);
    private final MGPref<Float> prefAlphaAtl = MGPref.get(R.string.FSATL_pref_alphaATL, 1.0f);
    private final MGPref<Boolean> prefStlVisibility = MGPref.get(R.string.FSATL_pref_STL_visibility, false);
    private final MGPref<Boolean> prefAtlVisibility = MGPref.get(R.string.FSATL_pref_ATL_visibility, false);
    private final MGPref<Boolean> prefMtlVisibility = MGPref.get(R.string.FSMarker_pref_MTL_visibility, false);

    private final MGPref<Boolean> prefHideStl = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHideAtl = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHideAll = MGPref.get(R.string.FSATL_pref_hideAll, false);
    private final MGPref<Boolean> prefHideAllEnabled = MGPref.anonymous(false);

    private ViewGroup dashboardStl = null;
    private ViewGroup dashboardStls = null;

    public FSAvailableTrackLogs(MGMapActivity mmActivity) {
        super(mmActivity);
        prefHideStl.addObserver((o, arg) -> getApplication().availableTrackLogsObservable.removeSelected());
        prefHideAtl.addObserver((o, arg) -> getApplication().availableTrackLogsObservable.removeUnselected());
        prefHideAll.addObserver((o, arg) -> getApplication().availableTrackLogsObservable.removeAll());
        Observer hideAllEnabledObserver = (o, arg) -> prefHideAllEnabled.setValue( prefStlVisibility.getValue() || prefAtlVisibility.getValue() || prefMtlVisibility.getValue() );
        prefStlVisibility.addObserver(hideAllEnabledObserver);
        prefAtlVisibility.addObserver(hideAllEnabledObserver);
        prefMtlVisibility.addObserver(hideAllEnabledObserver);

        getApplication().availableTrackLogsObservable.addObserver(refreshObserver);
        prefAlphaStl.addObserver(refreshObserver);
        prefAlphaAtl.addObserver(refreshObserver);
        prefStlGl.addObserver(refreshObserver);

    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        getControlView().setViewGroupColors(dvg, R.color.WHITE, R.color.BLUE100_A100);
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
            lsl.initPrefData(prefStlVisibility, prefAlphaStl, CC.getColor(R.color.BLUE), "SelectedTrackLog");
        }
        if ("atl".equals(info)) {
            lsl.initPrefData(prefAtlVisibility, prefAlphaAtl, CC.getColor(R.color.GREEN), "AvailableTrackLog");
        }
        return lsl;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("hide_stl".equals(info)){
            etv.setData(R.drawable.hide_stl);
            etv.setPrAction(prefHideStl);
            etv.setDisabledData(prefStlVisibility, R.drawable.hide_stl_dis);
            etv.setHelp(r(R.string.FSATL_qcHideStl_Help));
        } else if ("hide_atl".equals(info)){
            etv.setData(R.drawable.hide_atl);
            etv.setPrAction(prefHideAtl);
            etv.setDisabledData(prefAtlVisibility,R.drawable.hide_atl_dis);
            etv.setHelp(r(R.string.FSATL_qcHideAtl_Help));
        } else if ("hide_all".equals(info)){
            etv.setData(R.drawable.hide_all);
            etv.setPrAction(prefHideAll);
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
        for (TrackLog trackLog: available.getAvailableTrackLogs()){
            if ((trackLog != available.getSelectedTrackLogRef().getTrackLog()) &&
                    (trackLog != getApplication().recordingTrackLogObservable.getTrackLog()) &&
                    (trackLog != getApplication().markerTrackLogObservable.getTrackLog()) &&
                    (trackLog != getApplication().routeTrackLogObservable.getTrackLog())  ) {
                showTrack(trackLog, CC.getAlphaClone(PAINT_STROKE_ATL, prefAlphaAtl.getValue()), false);
                bAtlAlphaVisibility = true;
            }
        }
        prefAtlVisibility.setValue(bAtlAlphaVisibility);
        // handle selected track last, to get it on top
        TrackLog trackLog = available.getSelectedTrackLogRef().getTrackLog();
        if (trackLog!= null){
            Assert.check(available.getAvailableTrackLogs().contains(trackLog));
            if (prefStlGl.getValue()){
                CC.initGlPaints( prefAlphaStl.getValue() );
                showTrack(trackLog, CC.getAlphaClone(PAINT_STROKE_STL_GL, prefAlphaStl.getValue()), true);
            } else {
                showTrack(trackLog, CC.getAlphaClone(PAINT_STROKE_STL, prefAlphaStl.getValue()), false);
            }
            getControlView().setDashboardValue(true, dashboardStl ,trackLog.getTrackStatistic());
            int segIdx = available.getSelectedTrackLogRef().getSegmentIdx();
            getControlView().setDashboardValue(trackLog.getNumberOfSegments()>1, dashboardStls,(segIdx>=0)?trackLog.getTrackLogSegment(segIdx).getStatistic():null);
            TrackLogRef ref = available.getSelectedTrackLogRef();
            if (ref instanceof TrackLogRefZoom) {
                TrackLogRefZoom trackLogRefZoom = (TrackLogRefZoom) ref;
                if (trackLogRefZoom.isZoomForBB()){
                    trackLogRefZoom.setZoomForBB(false); // should be applied only once
                    getMapViewUtility().zoomForBoundingBox(trackLog.getBBox());
                }
            }
        }
        prefStlVisibility.setValue((trackLog != null) && (trackLog.getTrackStatistic().getNumPoints() >=2));

    }

    private void hideAvailableTrackLogs(){
        unregisterAll();
        getControlView().setDashboardValue(false,dashboardStl,null);
        getControlView().setDashboardValue(false,dashboardStls,null);
    }

    public Control[] getMenuLoadControls(){
        return new Control[]{
                new LoadPrevControl(),
                new LoadNextControl()};
    }

    public Control[] getMenuHideControls(){
        return new Control[]{
                new HideAllControl(),
                new HideSelectedControl(),
                new HideUnselectedControl(),
                new MarkerTrackHideControl2()};
    }



    public boolean loadFromBB(BBox bBox2Load){
        boolean changed = false;
        BBox bBox2show = new BBox();
        if (bBox2Load != null){
            for (TrackLog aTrackLog : getApplication().metaTrackLogs.values()){
                if (MetaDataUtil.checkLaLoRecords(aTrackLog, bBox2Load)){
                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(aTrackLog);
                    bBox2show.extend(aTrackLog.getBBox());
                    changed = true;
                }
            }
            if (changed){
                getApplication().availableTrackLogsObservable.changed();
//                getMapViewUtility().zoomForBoundingBox(bBox2show);
            }
        }
        return changed;
    }
}
