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
import mg.mapviewer.util.pref.MGPref;

public class MSAvailableTrackLogs extends MGMicroService {

    private final Paint PAINT_STROKE_ATL = CC.getStrokePaint(R.color.GREEN, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL = CC.getStrokePaint(R.color.BLUE, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_STL_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private final MGPref<Boolean> prefStlGl = MGPref.get(R.string.MSATL_pref_stlGl_key, true);

    public MSAvailableTrackLogs(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {
        getApplication().availableTrackLogsObservable.addObserver(refreshObserver);
    }

    @Override
    protected void stop() {
        getApplication().availableTrackLogsObservable.deleteObserver(refreshObserver);
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
        for (TrackLog trackLog: available.getAvailableTrackLogs()){
            if (available.getSelectedTrackLogRef().getTrackLog() != trackLog) {
                showTrack(trackLog, PAINT_STROKE_ATL, false);
            }
        }
        // handle selected track last, to get it on top
        TrackLog trackLog = available.getSelectedTrackLogRef().getTrackLog();
        if (trackLog!= null){
            Assert.check(available.getAvailableTrackLogs().contains(trackLog));
            if (prefStlGl.getValue()){
                showTrack(trackLog, PAINT_STROKE_STL_GL, true);
            } else {
                showTrack(trackLog, PAINT_STROKE_STL, false);
            }
            getControlView().showHideUpdateDashboard(true, R.id.stl ,trackLog.getTrackStatistic());
            int segIdx = available.getSelectedTrackLogRef().getSegmentIdx();
            getControlView().showHideUpdateDashboard(trackLog.getNumberOfSegments()>1, R.id.stls,(segIdx>=0)?trackLog.getTrackLogSegment(segIdx).getStatistic():null);
            TrackLogRef ref = available.getSelectedTrackLogRef();
            if (ref instanceof TrackLogRefZoom) {
                TrackLogRefZoom trackLogRefZoom = (TrackLogRefZoom) ref;
                if (trackLogRefZoom.isZoomForBB()){
                    trackLogRefZoom.setZoomForBB(false); // should be applied only once
                    getMapViewUtility().zoomForBoundingBox(trackLog.getBBox());
                }
            }
        }
    }

    private void hideAvailableTrackLogs(){
        unregisterAll();
        getControlView().showHideUpdateDashboard(false,R.id.stl,null);
        getControlView().showHideUpdateDashboard(false,R.id.stls,null);
    }

    public boolean selectCloseTrack(PointModel pmTap) {
        TreeSet<TrackLog> trackLogs = getApplication().availableTrackLogsObservable.availableTrackLogs;
        if (trackLogs.size() < 1) return false;

        TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1);
        bestMatch.setDistance(getMapViewUtility().getCloseThreshouldForZoomLevel());

        for (TrackLog tl : trackLogs){
            TrackLogRefApproach currentMatch = tl.getBestDistance(pmTap,bestMatch.getDistance());
            if (currentMatch != null){
                bestMatch = currentMatch;
            }
        }
        if ( bestMatch.getTrackLog() != null ){
            getApplication().availableTrackLogsObservable.setSelectedTrackLogRef(bestMatch);
            return true;
        }
        return false;
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
                new HideUnselectedControl()};

    }



    public void loadFromBB(BBox bBox2Load){
        boolean changed = false;
        BBox bBox2show = new BBox();
        if (bBox2Load != null){
            for (TrackLog aTrackLog : getApplication().metaTrackLogs){
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

    }
}
