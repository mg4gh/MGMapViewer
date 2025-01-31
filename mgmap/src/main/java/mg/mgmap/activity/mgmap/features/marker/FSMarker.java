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
package mg.mgmap.activity.mgmap.features.marker;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.model.DisplayModel;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import mg.mgmap.R;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;

public class FSMarker extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final long TT_HIDE_TIME = 10000;

    private volatile int ttCount = 0;
    private final Paint PAINT_STROKE_MTL = CC.getStrokePaint(R.color.CC_PINK, DisplayModel.getDeviceScaleFactor()*1.5f);

    private final Pref<Boolean> toggleEditMarkerTrack =  new Pref<>(false); // need separate action pref, since jump back to menu qc is an observer to this - otherwise timeout on editMarkerTrack triggers  jump back to menu group.
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);

    private final Pref<Float> prefAlphaMtl = getPref(R.string.FSMarker_pref_alphaMTL, 0.0f);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Boolean> triggerHideMtl = new Pref<>(false);
    private final Pref<Boolean> triggerHideAll = getPref(R.string.FSATL_pref_hideAll, false);
    private final Pref<Boolean> triggerReverseMtl = new Pref<>(false);
    private final Pref<Boolean> prefCalcRouteInProgress = getPref(R.string.FSRouting_pref_calcRouteInProgress, false);

    final MGMapApplication.TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = getApplication().markerTrackLogObservable;

   /**
     * MtlSupportProvider allows other implementation to be injected:
     *  - support to check for close lines
     *  - for snap2Way after manual operation (zoom level dependent)
    *   - to allow visualisation of dnd via extra line
     */
    public MtlSupportProvider mtlSupportProvider = new SimpleMtlSupportProvider();

    public FSMarker(MGMapActivity mmActivity) {
        super(mmActivity);
        toggleEditMarkerTrack.addObserver((e) -> prefEditMarkerTrack.toggle());
        prefEditMarkerTrack.addObserver((e) -> checkStartStopMCL());

        ttRefreshTime = 20;
        markerTrackLogObservable.addObserver(refreshObserver);
        prefAlphaMtl.addObserver(refreshObserver);

        Observer hideMarkerTrackObserver = (e) -> {
            getApplication().markerTrackLogObservable.setTrackLog(null);
            prefEditMarkerTrack.setValue(false);
        };
        triggerHideMtl.addObserver(hideMarkerTrackObserver);
        triggerHideAll.addObserver(hideMarkerTrackObserver);
        prefEditMarkerTrack.setValue(false);
        triggerReverseMtl.addObserver((e)->createMarkerTrackLog(markerTrackLogObservable.getTrackLog(), true));
    }

    private final Runnable ttHide = () -> {
        if (prefCalcRouteInProgress.getValue()){ // if routing is still in progress start timer from scratch
            refreshTTHide();
        } else {
            if (ttCount <= 1){ // if timer is expired, switch of prefEditMarkerTrack
                prefEditMarkerTrack.setValue(false);
            } else { // trigger next timer period
                refreshTTHide(ttCount-1);
            }
        }
    };
    private void refreshTTHide(){
        refreshTTHide(6);
    }
    private void refreshTTHide(int count){
        ttCount = count;
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide, TT_HIDE_TIME);
    }


    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("mtl".equals(info)) {
            lsl.initPrefData(prefMtlVisibility, prefAlphaMtl, CC.getColor(R.color.CC_PINK), "MarkerTrackLog");
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
        } else if ("reverse".equals(info)){
            etv.setData(R.drawable.marker_reverse);
            etv.setPrAction(triggerReverseMtl);
            etv.setDisabledData(prefMtlVisibility,R.drawable.marker_reverse_dis);
            etv.setHelp(r(R.string.FSMarker_qcReverseMtl_Help));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (markerTrackLogObservable.getTrackLog() != null){
            refreshObserver.onChange();
        }
    }

    @Override
    protected void onPause() {
        prefEditMarkerTrack.setValue(false);
        unregisterAllControl();
        super.onPause();
        getTimer().removeCallbacks(ttHide);
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
            register(new MarkerControlLayer());
        } else {
            unregisterAllControl();
        }
    }

    public void createMarkerTrackLog(TrackLog trackLog){
        createMarkerTrackLog(trackLog, false);
    }

    public void createMarkerTrackLog(TrackLog trackLog, boolean reverse){
        String name = trackLog.getName();
        name = name.endsWith("_MarkerTrack")?name:(name+"_MarkerTrack");
        WriteableTrackLog mtl = trackLog.cloneTrackLog(reverse);
        mtl.setName(name);
        markerTrackLogObservable.setTrackLog(mtl);
        getMapViewUtility().zoomForBoundingBox(trackLog.getBBox());
        refreshObserver.onChange();
    }

    private void initMarkerTrackLog(){
        long now = System.currentTimeMillis();
        WriteableTrackLog mtl = new WriteableTrackLog(Formatter.SDF.format(new Date(now))+"_MarkerTrack");
        mtl.startTrack(now);
        mtl.startSegment(now);
        markerTrackLogObservable.setTrackLog(mtl);
    }


    private void showHide(WriteableTrackLog mtl){
        unregisterAll();
        boolean bMtlAlphaVisibility = false;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() >= 1)){
            showTrack(mtl, CC.getAlphaClone(PAINT_STROKE_MTL, prefAlphaMtl.getValue()), false, (int)(DisplayModel.getDeviceScaleFactor()*5.0f), true);
            bMtlAlphaVisibility = true;
        }
        prefMtlVisibility.setValue( bMtlAlphaVisibility );
    }



    public class MarkerControlLayer extends ControlMVLayer<TrackLogRefApproach> {

        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            double radius = getRadiusForMarkerActions();
            TrackLogRefApproach pointRef = mtl.getBestPoint(pmTap, radius);
            if (pointRef != null){
                deleteMarkerPoint(mtl, pointRef.getSegmentIdx(), pointRef.getEndPointIndex());
            } else {
                TrackLogRefApproach lineRef = mtlSupportProvider.getBestDistance(mtl,pmTap, radius);
                mtlSupportProvider.optimizePosition(pmTap, radius);
                if (lineRef != null){
                    if (mtl.getBestPoint(lineRef.getApproachPoint(), radius) == null){ // if approachPoint is too close to other point, then don't insert
                        insertPoint(mtl, lineRef, pmTap);
                    }
                } else {
                    if (mtl.getBestPoint(pmTap, radius) == null){ // if optimized pos is too close to other point, then don't add
                        addPoint(mtl, pmTap);
                    }
                }
            }
            mtl.recalcStatistic();
            mtl.setModified(true);
            markerTrackLogObservable.changed();
            return true;
        }

        @Override
        protected boolean checkDrag(PointModel pmStart) {
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            double radius = getRadiusForMarkerActions();
            TrackLogRefApproach pointRef = mtl.getBestPoint(pmStart, radius);
            TrackLogRefApproach lineRef = mtlSupportProvider.getBestDistance(mtl, pmStart, radius);
            mgLog.i(pmStart);
            try {
                if (pointRef == null){
                    if (lineRef != null){
                        if (mtl.getBestPoint(lineRef.getApproachPoint(), radius) == null){ // if approachPoint is too close to other point, then don't insert
                            WriteablePointModel wpm = new WriteablePointModelImpl(lineRef.getApproachPoint());
                            mtlSupportProvider.optimizePosition(wpm, radius);
                            insertPoint(mtl, lineRef, wpm);
                            pointRef = mtl.getBestPoint(wpm, radius);
                        }
                    }
                }
                if (pointRef != null){
                    setDragObject(pointRef);

                    if (getMapView().getModel().mapViewPosition.getZoomLevel() < 15){
                        // if prev of next point are also close, then prohibit drag - if too much points close, the user intention is not clear
                        TrackLogSegment segment = mtl.getTrackLogSegment(pointRef.getSegmentIdx());
                        int tlpIdx = pointRef.getEndPointIndex();
                        if (tlpIdx > 0){ // there is a previous point -> check whether also close
                            PointModel prevPoint = segment.get(tlpIdx-1);
                            if (getMapViewUtility().isClose( PointModelUtil.distance(pmStart, prevPoint) )){
                                setDragObject(null);
                            }
                        }
                        if (tlpIdx < segment.size()-1){
                            PointModel nextPoint = segment.get(tlpIdx+1);
                            if (getMapViewUtility().isClose( PointModelUtil.distance(pmStart, nextPoint) )){
                                setDragObject(null);
                            }
                        }
                    }
                }
                if (getDragObject() != null){
                    mgLog.i("dragObject="+getDragObject());
                }
            } catch (Exception e){
                setDragObject(null);
            }
            return (getDragObject() != null);
        }

        @Override
        protected void handleDrag(WriteablePointModel pmCurrent) {
            mgLog.i("pmCurrent="+pmCurrent);
            WriteableTrackLog mtl = markerTrackLogObservable.getTrackLog();
            TrackLogRefApproach dragRef = getDragObject();
            mtlSupportProvider.optimizePosition(pmCurrent, getRadiusForMarkerActions());
            moveMarkerPoint(mtl, dragRef.getSegmentIdx(), dragRef.getEndPointIndex(), pmCurrent);
            mtl.recalcStatistic();
            mtl.setModified(true);
            markerTrackLogObservable.changed();
        }

    }


    private double getRadiusForMarkerActions(){
        return getMapViewUtility().getCloseThresholdForZoomLevel(500);
    }

    private void moveMarkerPoint(TrackLog mtl, int segIdx, int tlpIdx, WriteablePointModel pos){
        getApplication().getElevationProvider().setElevation(pos);
        TrackLogSegment segment = mtl.getTrackLogSegment(segIdx);
        segment.movePoint(tlpIdx, pos);
        mtlSupportProvider.pointMovedCallback(segment.get(tlpIdx));
    }

    private void deleteMarkerPoint(WriteableTrackLog mtl, int segIdx, int tlpIdx){
        TrackLogSegment segment = mtl.getTrackLogSegment(segIdx);
        PointModel mtlp = segment.removePoint(tlpIdx);
        mtlSupportProvider.pointDeletedCallback(mtlp);
    }

    private void addPoint(WriteableTrackLog mtl, WriteablePointModel pmTap){
        getApplication().getElevationProvider().setElevation(pmTap);
        mtl.addPoint( pmTap );
        mtlSupportProvider.pointAddedCallback( pmTap );
    }

    private void insertPoint(WriteableTrackLog mtl, TrackLogRefApproach lineRef, WriteablePointModel pos) {
        assert (lineRef.getTrackLog() == mtl);
        TrackLogSegment segment = mtl.getTrackLogSegment(lineRef.getSegmentIdx());
        int tlpIdx = lineRef.getEndPointIndex();
        getApplication().getElevationProvider().setElevation(pos);
        segment.addPoint(tlpIdx, pos);
        mtlSupportProvider.pointAddedCallback( pos );
    }

    public interface MtlSupportProvider{
        TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) ;
        default void optimizePosition(WriteablePointModel wpm, double threshold) {}
        default void pointAddedCallback(PointModel pm) {}
        default void pointMovedCallback(PointModel pm) {}
        default void pointDeletedCallback(PointModel pm) {}
    }
    public static class SimpleMtlSupportProvider implements MtlSupportProvider{
        public TrackLogRefApproach getBestDistance( WriteableTrackLog mtl, PointModel pm, double threshold) {
            return mtl.getBestDistance(pm,threshold);
        }
    }
}
