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
package mg.mapviewer.features.atl;

import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;

import java.util.TreeSet;

import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.util.Assert;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.MetaDataUtil;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.LabeledSlider;

public class MSAvailableTrackLogs extends MGMicroService {

    private final Paint PAINT_STROKE_ATL = CC.getStrokePaint(R.color.GREEN, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL = CC.getStrokePaint(R.color.BLUE, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private final MGPref<Boolean> prefStlGl = MGPref.get(R.string.MSATL_pref_stlGl_key, true);
    private final MGPref<Float> prefAlphaStl = MGPref.get(R.string.MSATL_pref_alphaSTL , 1.0f);
    private final MGPref<Float> prefAlphaAtl = MGPref.get(R.string.MSATL_pref_alphaATL, 1.0f);
    private final MGPref<Boolean> prefAlphaStlVisibility = MGPref.get(R.string.MSATL_pref_alphaSTL_visibility, false);
    private final MGPref<Boolean> prefAlphaAtlVisibility = MGPref.get(R.string.MSATL_pref_alphaATL_visibility, false);


    private ViewGroup dashboardStl = null;
    private ViewGroup dashboardStls = null;

    public MSAvailableTrackLogs(MGMapActivity mmActivity) {
        super(mmActivity);
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
            lsl.initPrefData(prefAlphaStlVisibility, prefAlphaStl, CC.getColor(R.color.BLUE), "SelectedTrackLog");
        }
        if ("atl".equals(info)) {
            lsl.initPrefData(prefAlphaAtlVisibility, prefAlphaAtl, CC.getColor(R.color.GREEN), "AvailableTrackLog");
        }
        return lsl;
    }

    @Override
    protected void start() {
        getApplication().availableTrackLogsObservable.addObserver(refreshObserver);
        prefAlphaStl.addObserver(refreshObserver);
        prefAlphaAtl.addObserver(refreshObserver);
        prefStlGl.addObserver(refreshObserver);
    }

    @Override
    protected void stop() {
        getApplication().availableTrackLogsObservable.deleteObserver(refreshObserver);
        prefAlphaStl.deleteObserver(refreshObserver);
        prefAlphaAtl.deleteObserver(refreshObserver);
        prefStlGl.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAvailableTrackLogs();
            }
        });
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
//                showTrack(trackLog, PAINT_STROKE_ATL, false);
                showTrack(trackLog, CC.getAlphaClone(PAINT_STROKE_ATL, prefAlphaAtl.getValue()), false);
                bAtlAlphaVisibility = true;
            }
        }
        prefAlphaAtlVisibility.setValue(bAtlAlphaVisibility);
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
        prefAlphaStlVisibility.setValue((trackLog != null) && (trackLog.getTrackStatistic().getNumPoints() >=2));
    }

    private void hideAvailableTrackLogs(){
        unregisterAll();
        getControlView().setDashboardValue(false,dashboardStl,null);
        getControlView().setDashboardValue(false,dashboardStls,null);
    }

//    public TrackLogRefApproach selectCloseTrack(PointModel pmTap) {
//        TreeSet<TrackLog> trackLogs = getApplication().availableTrackLogsObservable.availableTrackLogs;
////        if (trackLogs.size() < 1) return false;
//
//        TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1);
//        bestMatch.setDistance(getMapViewUtility().getCloseThreshouldForZoomLevel());
//
//        for (TrackLog tl : trackLogs){
//            TrackLogRefApproach currentMatch = tl.getBestDistance(pmTap,bestMatch.getDistance());
//            if (currentMatch != null){
//                bestMatch = currentMatch;
//            }
//        }
//        return bestMatch;
////        if ( bestMatch.getTrackLog() != null ){
////            getApplication().availableTrackLogsObservable.setSelectedTrackLogRef(bestMatch);
////            return true;
////        }
////        return false;
//    }

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
