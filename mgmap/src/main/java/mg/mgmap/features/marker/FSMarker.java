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
package mg.mgmap.features.marker;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.model.DisplayModel;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Observer;

import mg.mgmap.R;
import mg.mgmap.graph.GGraphTile;
import mg.mgmap.model.WriteableTrackLog;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.model.TrackLogRefApproach;
import mg.mgmap.model.TrackLogSegment;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.model.WriteablePointModelImpl;
import mg.mgmap.util.AltitudeProvider;
import mg.mgmap.util.Assert;
import mg.mgmap.util.CC;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.LabeledSlider;
import mg.mgmap.view.MVLayer;

public class FSMarker extends FeatureService {

    private final Paint PAINT_STROKE_MTL = CC.getStrokePaint(R.color.PINK, DisplayModel.getDeviceScaleFactor()*1.5f);

    private final Pref<Boolean> toggleEditMarkerTrack =  new Pref<>(false); // need separate action pref, since jump back to menu qc is an observer to this - otherwise timeout on editMarkerTrack triggers  jump back to menu group.
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    private final Pref<Boolean> prefAutoMarkerSetting = getPref(R.string.FSMarker_pref_auto_key, true);

    private final Pref<Float> prefAlphaMtl = getPref(R.string.FSMarker_pref_alphaMTL, 1.0f);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Boolean> triggerHideMtl = new Pref<>(false);
    private final Pref<Boolean> triggerHideAll = getPref(R.string.FSATL_pref_hideAll, false);
    private final Pref<Boolean> prefAutoSwitcher = getPref(R.string.FSMarker_pref_auto_switcher, true);

    final MGMapApplication.TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = getApplication().markerTrackLogObservable;

    public FSMarker(MGMapActivity mmActivity) {
        super(mmActivity);
        toggleEditMarkerTrack.addObserver((o, arg) -> prefEditMarkerTrack.toggle());
        prefEditMarkerTrack.addObserver((o, arg) -> checkStartStopMCL());

        prefAutoSwitcher.addObserver((o, arg) -> {
            if (prefAutoMarkerSetting.getValue()) {
                boolean smallMtl = prefAutoSwitcher.getValue();
                prefAlphaMtl.setValue(smallMtl ? 0.0f : 1.0f);
            }
        });

        ttRefreshTime = 20;
        markerTrackLogObservable.addObserver(refreshObserver);
        prefAlphaMtl.addObserver(refreshObserver);

        Observer hideMarkerTrackObserver = (o, arg) -> {
            getApplication().markerTrackLogObservable.setTrackLog(null);
            GGraphTile.clearCache();
            prefEditMarkerTrack.setValue(false);
        };
        triggerHideMtl.addObserver(hideMarkerTrackObserver);
        triggerHideAll.addObserver(hideMarkerTrackObserver);
    }

    public LineRefProvider lineRefProvider = new MarkerLineRefProvider(); // support to check for close lines - other implementation can be injected

    private final Runnable ttHide = () -> prefEditMarkerTrack.setValue(false);
    long ttHideTime = 15000;
    private void refreshTTHide(){
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide,ttHideTime);
    }


    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("mtl".equals(info)) {
            lsl.initPrefData(prefMtlVisibility, prefAlphaMtl, CC.getColor(R.color.PINK), "MarkerTrackLog");
        }
        return lsl;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("markerEdit".equals(info)){
            etv.setData(prefEditMarkerTrack,R.drawable.mtlr2, R.drawable.mtlr);
            etv.setPrAction(toggleEditMarkerTrack);
            etv.setHelp(r(R.string.FSMarker_qcEditMarkerTrack_Help)).setHelp(r(R.string.FSMarker_qcEditMarkerTrack_Help1),r(R.string.FSMarker_qcEditMarkerTrack_Help2));
        } else if ("hide_mtl".equals(info)){
            etv.setData(R.drawable.hide_mtl);
            etv.setPrAction(triggerHideMtl);
            etv.setDisabledData(prefMtlVisibility,R.drawable.hide_mtl_dis);
            etv.setHelp(r(R.string.FSMarker_qcHideMtl_Help));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefEditMarkerTrack.setValue(false);
        if (markerTrackLogObservable.getTrackLog() != null){
            refreshObserver.onChange();
        }
    }

    @Override
    protected void onPause() {
        unregisterClass(MarkerControlLayer.class);
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        showHide(markerTrackLogObservable.getTrackLog());
        refreshTTHide();
    }


    private void checkStartStopMCL(){
        if (prefEditMarkerTrack.getValue()){
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            if (mtl == null){
                initMarkerTrackLog();
            } else {
                markerTrackLogObservable.changed();
            }
            register(new MarkerControlLayer(), false);
        } else {
            unregisterClass(MarkerControlLayer.class);
        }
    }


    public void createMarkerTrackLog(TrackLog trackLog){
        setAutoSwitcher(trackLog.getTrackStatistic().getNumPoints() < 200); // < 200 means rather small MTL with corresponding settings
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
        setAutoSwitcher(true);
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);
        long now = System.currentTimeMillis();
        WriteableTrackLog mtl = new WriteableTrackLog(sdf2.format(new Date(now))+"_MarkerTrack");
        mtl.startTrack(now);
        mtl.startSegment(now);
        markerTrackLogObservable.setTrackLog(mtl);
    }


    private void showHide(WriteableTrackLog mtl){
        if (!fsLayers.isEmpty()){
            unregisterAll();
        }
        boolean bMtlAlphaVisibility = false;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() >= 1)){
            showTrack(mtl, CC.getAlphaClone(PAINT_STROKE_MTL, prefAlphaMtl.getValue()), false, (int)(DisplayModel.getDeviceScaleFactor()*5.0f), true);
            bMtlAlphaVisibility = true;
        }
        prefMtlVisibility.setValue( bMtlAlphaVisibility );
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

    public interface LineRefProvider{
        TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) ;
    }
    public static class MarkerLineRefProvider implements LineRefProvider{
        public TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) {
            return mtl.getBestDistance(pm,threshold);
        }
    }

    private void setAutoSwitcher(boolean bValue){
        prefAutoSwitcher.setValue(bValue);
        prefAutoSwitcher.onChange();
    }
}
