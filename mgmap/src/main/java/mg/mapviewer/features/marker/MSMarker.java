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
package mg.mapviewer.features.marker;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.model.DisplayModel;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;

import mg.mapviewer.R;
import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteablePointModelImpl;
import mg.mapviewer.util.AltitudeProvider;
import mg.mapviewer.util.Assert;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.LabeledSlider;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.view.PrefTextView;

public class MSMarker extends MGMicroService {

    private final Paint PAINT_STROKE_MTL = CC.getStrokePaint(R.color.PINK, DisplayModel.getDeviceScaleFactor()*1.5f);

    private final MGPref<Boolean> prefEditMarkerTrack =  MGPref.get(R.string.MSMarker_qc_EditMarkerTarck, false);
    private final MGPref<Boolean> prefAutoMarkerSetting = MGPref.get(R.string.MSMarker_pref_auto_key, true);
//    private final MGPref<Boolean> prefShowMtl = MGPref.get(R.string.MSMarker_pref_showMtl_key, false);
    private final MGPref<Boolean> prefSnap2Way = MGPref.get(R.string.MSMarker_pref_snap2way_key, true);

    private final MGPref<Float> prefAlphaMtl = MGPref.get(R.string.MSMarker_pref_alphaMTL, 1.0f);
    private final MGPref<Boolean> prefAlphaMtlVisibility = MGPref.get(R.string.MSMarker_pref_alphaMTL_visibility, false);
    private final MGPref<Float> prefAlphaRotl = MGPref.get(R.string.MSRouting_pref_alphaRoTL, 1.0f);

    MGMapApplication.TrackLogObservable<WriteableTrackLog> markerTrackLogObservable;

    public MSMarker(MGMapActivity mmActivity) {
        super(mmActivity);
        editMarkerTrackObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                checkStartStopMCL();
            }
        };
    }

    private final Observer editMarkerTrackObserver;
    public LineRefProvider lineRefProvider = new MarkerLineRefProvider(); // support to check for close lines - other implementation can be injected

    private TimerTask ttHide = new TimerTask() {
        @Override
        public void run() {
            prefEditMarkerTrack.setValue(false);
        }
    };
    long ttHideTime = 15000;
    private void refreshTTHide(){
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide,ttHideTime);
    }

    @Override
    public PrefTextView initQuickControl(PrefTextView ptv, String info) {
        ptv.setPrefData(new MGPref[]{prefEditMarkerTrack},
                new int[]{R.drawable.mtlr, R.drawable.mtlr2});
        return ptv;
    }

    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("mtl".equals(info)) {
            lsl.initPrefData(prefAlphaMtlVisibility, prefAlphaMtl, CC.getColor(R.color.PINK), "MarkerTrackLog");
        }
        return lsl;
    }

    @Override
    protected void start() {
        super.start();
        prefEditMarkerTrack.setValue(false);
        prefEditMarkerTrack.addObserver(editMarkerTrackObserver);

        markerTrackLogObservable = getApplication().markerTrackLogObservable;
        WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
        if (mtl != null){
            refreshObserver.onChange();
        }
        markerTrackLogObservable.addObserver(refreshObserver);
//        getMapView().getModel().mapViewPosition.addObserver(refreshObserver);
        ttRefreshTime = 20;

        prefAlphaMtl.addObserver(refreshObserver);
    }

    @Override
    protected void stop() {
        markerTrackLogObservable.deleteObserver(refreshObserver);
        getMapView().getModel().mapViewPosition.removeObserver(refreshObserver);
        prefEditMarkerTrack.deleteObserver(editMarkerTrackObserver);
        unregisterClass(MarkerControlLayer.class);

        prefAlphaMtl.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        showHide(markerTrackLogObservable.getTrackLog());
        refreshTTHide();
    }


    private void checkStartStopMCL(){
        if (prefEditMarkerTrack.getValue()){
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            if (mtl == null){
                initMarkerTrackLog();
            }
            register(new MarkerControlLayer(), false);
            markerTrackLogObservable.changed();
        } else {
            unregisterClass(MarkerControlLayer.class);
        }
    }


    public void createMarkerTrackLog(TrackLog trackLog){
        adaptAutoSettings(trackLog.getTrackStatistic().getNumPoints() < 200);
        WriteableTrackLog mtl = new WriteableTrackLog(trackLog.getName()+"__MarkerTrack");
        mtl.startTrack(trackLog.getTrackStatistic().getTStart());
        for (TrackLogSegment segment : trackLog.getTrackLogSegments()){
            mtl.startSegment(segment.getStatistic().getTStart());
            for (int i = 0; i<segment.size(); i++){
                PointModel pm = segment.get(i);
                PointModel npm;
                if (pm instanceof TrackLogPoint) {
                    npm = new TrackLogPoint((TrackLogPoint) pm);
                } else {
                    npm = new WriteablePointModelImpl(pm);
                }
                mtl.addPoint(npm);
            }
            mtl.stopSegment(segment.getStatistic().getTEnd());
        }
        mtl.stopTrack(trackLog.getTrackStatistic().getTEnd());
        markerTrackLogObservable.setTrackLog(mtl);
        getMapViewUtility().zoomForBoundingBox(trackLog.getBBox());
        showHide(mtl);
    }

    private void initMarkerTrackLog(){
        adaptAutoSettings(true);
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);
        long now = System.currentTimeMillis();
        WriteableTrackLog mtl = new WriteableTrackLog(sdf2.format(new Date(now))+"_MarkerTrack");
        mtl.startTrack(now);
        mtl.startSegment(now);
        markerTrackLogObservable.setTrackLog(mtl);
    }


    private void showHide(WriteableTrackLog mtl){
        if (!msLayers.isEmpty()){
            unregisterAll();
        }
        boolean bMtlAlphaVisibility = false;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() >= 1)){
            showTrack(mtl, CC.getAlphaClone(PAINT_STROKE_MTL, prefAlphaMtl.getValue()), false, (int)(DisplayModel.getDeviceScaleFactor()*5.0f), true);
            bMtlAlphaVisibility = true;
        }
        prefAlphaMtlVisibility.setValue( bMtlAlphaVisibility );
    }



    public class MarkerControlLayer extends MVLayer {

        private MarkerControlLayer(){
            setDragging();
        }

        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            TrackLogRefApproach pointRef = mtl.getBestPoint(pmTap, getMapViewUtility().getCloseThreshouldForZoomLevel());
            TrackLogRefApproach lineRef = lineRefProvider.getBestDistance(mtl,pmTap, getMapViewUtility().getCloseThreshouldForZoomLevel());

            if (pointRef != null){
                deleteMarkerPoint(mtl, pointRef.getSegmentIdx(), pointRef.getEndPointIndex());
            } else {
                pmTap.setEle(AltitudeProvider.getAltitude(pmTap.getLat(), pmTap.getLon()));
                if (lineRef != null){
                    insertPoint(mtl, pmTap, lineRef);
                } else {
                    addPoint(mtl, pmTap);
                }
            }
            mtl.recalcStatistic();
            mtl.setModified(true);
            markerTrackLogObservable.changed();
            return true;
        }

        @Override
        protected boolean checkDrag(PointModel pmStart, DragData dragData) {
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            TrackLogRefApproach pointRef = mtl.getBestPoint(pmStart, getMapViewUtility().getCloseThreshouldForZoomLevel());
            TrackLogRefApproach lineRef = lineRefProvider.getBestDistance(mtl, pmStart, getMapViewUtility().getCloseThreshouldForZoomLevel());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+pmStart);
            try {
                if (pointRef == null){
                    if (lineRef != null){
                        insertPoint(mtl, pmStart, lineRef);
                        pointRef = mtl.getBestPoint(pmStart, getMapViewUtility().getCloseThreshouldForZoomLevel());
                    }
                }
                if (pointRef != null){
                    dragData.setDragObject(pointRef);

                    if (getMapView().getModel().mapViewPosition.getZoomLevel() < 15){
                        // if prev of next point are also close, then prohibit drag - if too much points close, the user intention is not clear
                        TrackLogSegment segment = mtl.getTrackLogSegment(pointRef.getSegmentIdx());
                        int tlpIdx = pointRef.getEndPointIndex();
                        if (tlpIdx > 0){ // there is a previous point -> check whether also close
                            PointModel prevPoint = segment.get(tlpIdx-1);
                            if (getMapViewUtility().isClose( PointModelUtil.distance(pmStart, prevPoint) )){
                                dragData.setDragObject(null);
                            }
                        }
                        if (tlpIdx < segment.size()-1){
                            PointModel nextPoint = segment.get(tlpIdx+1);
                            if (getMapViewUtility().isClose( PointModelUtil.distance(pmStart, nextPoint) )){
                                dragData.setDragObject(null);
                            }
                        }
                    }
                }
                if (dragData.getDragObject() != null){
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" dragData.dragObject="+dragData.getDragObject());
                }
            } catch (Exception e){
                dragData.setDragObject(null);
            }
            return (dragData.getDragObject() != null);
        }

        @Override
        protected void handleDrag(PointModel pmCurrent, DragData dragData) {
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" pmCurrent="+pmCurrent);
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            TrackLogRefApproach dragRef = dragData.getDragObject(TrackLogRefApproach.class);
            moveMarkerPoint(mtl, dragRef.getSegmentIdx(), dragRef.getEndPointIndex(), pmCurrent);
            mtl.recalcStatistic();
            mtl.setModified(true);
            markerTrackLogObservable.changed();
        }

    }




    private void moveMarkerPoint(TrackLog mtl, int segIdx, int tlpIdx, PointModel pos){
        PointModel pm = mtl.getTrackLogSegment(segIdx).get(tlpIdx);
        if (pm instanceof WriteablePointModel) {
            WriteablePointModel mtlp = (WriteablePointModel) pm;
            mtlp.setLat(pos.getLat());
            mtlp.setLon(pos.getLon());
        }
    }

    private void deleteMarkerPoint(WriteableTrackLog mtl, int segIdx, int tlpIdx){
        TrackLogSegment segment = mtl.getTrackLogSegment(segIdx);
        segment.removePoint(tlpIdx);
    }

    private void addPoint(WriteableTrackLog mtl, PointModel pmTap){
        mtl.addPoint( pmTap );
    }


    private void insertPoint(WriteableTrackLog mtl, PointModel pmTap, TrackLogRefApproach lineRef) {
        Assert.check(lineRef.getTrackLog() == mtl);
        TrackLogSegment segment = mtl.getTrackLogSegment(lineRef.getSegmentIdx());
        int tlpIdx = lineRef.getEndPointIndex();
        segment.addPoint(tlpIdx, pmTap);
    }

    public Control[] getMenuMarkerControls(){
        return new Control[]{
                new MarkerTrackHideControl(),
                new MarkerLoadSelectedControl(this),
                new MarkerTrackSaveControl()};
    }

    public interface LineRefProvider{
        TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) ;
    }
    public class MarkerLineRefProvider implements LineRefProvider{
        public TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) {
            return mtl.getBestDistance(pm,threshold);
        }
    }

    private void adaptAutoSettings(boolean smallMtl){
        if (prefAutoMarkerSetting.getValue()){
            prefAlphaMtl.setValue(smallMtl?0.0f:1.0f);
            prefSnap2Way.setValue(smallMtl);
            if (prefAlphaRotl.getValue() < 0.25f){
                prefAlphaRotl.setValue(1.0f);
            }
        }
    }
}
