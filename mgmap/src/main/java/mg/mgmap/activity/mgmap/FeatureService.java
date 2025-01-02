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

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Lifecycle;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.common.Observer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.CC;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.view.MVGroupLayer;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.activity.mgmap.view.MVLayer;
import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.activity.mgmap.view.MultiPointGLView;
import mg.mgmap.activity.mgmap.view.MultiPointView;

/**
 * <p>The concept of MicroServices helps to split the overall functionality into smaller parts. These parts are realized as features in separate packages and they are mostly independent from each other.</p>
 * <p>MicroServices are coupled to the LifeCycle of the MGMapActivity. They are started by the onResume method and they are stopped by the onPause Method of this activity.</p>
 *
 * <p>The default implementation provides a RefreshObserver that can be registered on relevant observables of the application. The RefreshObserver triggers a short running timer. Thus the MicroServices get
 * decoupled from the events - high frequency events are filtered out this way.</p>
 *
 * <p>A second feature of the MicroServices is the register/unregister functionality for MapView layers. Beside the registration in the MapView.Layers object these Layers get also a reference to the MapViewUtility and
 * (depending on a parameter) their reference is also stored in the layer registry of the service.</p>
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    /** A timer object. */
    private static final Handler timer = new Handler();

    protected final MGMapActivity activity;
    protected final MGMapApplication application;
    protected GroupLayer fsLayers = new GroupLayer();
    protected MVGroupLayer fsControlLayers = new MVGroupLayer();
    protected String logName;
    public static final double ENUMERATION_DISTANCE = 1000;

    public FeatureService(MGMapActivity activity){
        this.activity = activity;
        this.application = activity.application;
        logName = this.getClass().getSimpleName();
        getMapView().getLayerManager().getLayers().add(fsLayers);
        getMapView().getLayerManager().getLayers().add(fsControlLayers);
    }

    public class RefreshObserver implements Observer, PropertyChangeListener {

        public Object last;

        @Override
        public void onChange() {
            cancelRefresh();
            getTimer().postDelayed(ttRefresh,ttRefreshTime);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            last = event.getSource();
            mgLog.d("dest="+FeatureService.this.getClass().getSimpleName()+" source="+last+" name="+event.getPropertyName());
            onUpdate(event);
            onChange();
        }
    }

    protected void onUpdate(PropertyChangeEvent event){} //hook for derived classes

    protected long ttRefreshTime = 100;
    private final Runnable ttRefresh = this::doRefresh;
    protected RefreshObserver refreshObserver = new RefreshObserver();

    protected void doRefresh(){
        if (getActivity().getLifecycle().getCurrentState() == Lifecycle.State.RESUMED){
            doRefreshResumed();
        }
    }

    protected void doRefreshResumed(){
//        Log.v(MGMapApplication.LABEL, NameUtil.context() + " "+this.getClass().getName()+" doRefreshResumed");
        getActivity().runOnUiThread(this::doRefreshResumedUI);
    }
    protected void doRefreshResumedUI(){}
    protected void cancelRefresh(){
        getTimer().removeCallbacks(ttRefresh);
    }

    public ViewGroup initDashboard(ViewGroup dvg, String info){
        for (int i=0; i<dvg.getChildCount(); i++){
            if (dvg.getChildAt(i) instanceof ExtendedTextView child) {
                child.setName(info);
            }
        }
        return dvg;
    }
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info){
        return lsl;
    }
    public ExtendedTextView initStatusLine(ExtendedTextView etv, String info){
        etv.setName(info);
        return etv;
    }
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        etv.setName(info);
        return etv;
    }

    protected void showTrack(TrackLog trackLog, Pref<Boolean> glPref, Paint glPaint, Paint noGlPaint, float alpha, int pointRadius){
        if (trackLog != null){
            if (!trackLog.hasGainLoss()){
                glPref.setValue(false);
            }
            if (glPref.getValue()){
                CC.initGlPaints( alpha );
                showTrack(trackLog, CC.getAlphaClone(glPaint, alpha), true, pointRadius );
            } else {
                showTrack(trackLog, CC.getAlphaClone(noGlPaint, alpha), false, pointRadius );
            }
        }
    }

    protected void showTrack(TrackLog trackLog, Paint paint, boolean showGL, int pointRadius) {
        showTrack(trackLog,paint,showGL,pointRadius, false);
    }

    protected void showTrack(TrackLog trackLog, Paint paint, boolean showGL, int pointRadius, boolean showIntermediates) {
        this.showTrack(trackLog,paint, showGL, pointRadius, showIntermediates, false);
    }

    protected void showTrack(TrackLog trackLog, Paint paint, boolean showGL, int pointRadius, boolean showIntermediates, boolean enumeratePoints){
        if (trackLog == null) return;
        if (pointRadius < 0 ) pointRadius = MultiPointView.POINT_RADIUS;
        MultiPointModelImpl enumeratedPoints = new MultiPointModelImpl();
        double totalLength = 0;
        for (int idx = 0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            if (enumeratePoints && (segment.size() > 0)){
                PointModel lastPM = segment.get(0);
                for (PointModel pm : segment){
                    double length = PointModelUtil.distance(lastPM, pm);
                    for (int enumarationCnt = (int)(totalLength/ENUMERATION_DISTANCE)+1; enumarationCnt <= (int)((totalLength+length)/ENUMERATION_DISTANCE); enumarationCnt++ ){
                        enumeratedPoints.addPoint(PointModelUtil.interpolate(lastPM, pm, enumarationCnt*ENUMERATION_DISTANCE - totalLength));
                    }
                    lastPM = pm;
                    totalLength += length;
                }
            } else {
                MultiPointView layer = (showGL)?new MultiPointGLView(segment, paint):new MultiPointView(segment, paint);
                if (showGL) layer.setStrokeIncrease(1.35);
                layer.setPointRadius(pointRadius);
                layer.setShowIntermediates(showIntermediates);
                register(layer);
            }
        }
        if (enumeratePoints){
            MultiPointView layer = new MultiPointView(enumeratedPoints, paint);
            layer.setPointRadius(pointRadius);
            layer.setShowIntermediates(showIntermediates);
            layer.setEnumeratePoints(true);
            register(layer);
        }

    }


    protected void register(final Layer layer){
        if (layer == null) return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (layer instanceof MVLayer) {
                ((MVLayer) layer).setMapViewUtility(getMapViewUtility());
            }
            layer.setDisplayModel(fsLayers.getDisplayModel());
//            mgLog.d("register fs="+this.getClass().getSimpleName()+" layer="+layer.getClass().getSimpleName()+" control="+(layer instanceof ControlMVLayer));
            synchronized (getMapView().getLayerManager().getLayers()) {
                if (layer instanceof ControlMVLayer){
                    fsControlLayers.layers.add(layer);
                } else {
                    fsLayers.layers.add(layer);
                }
                getMapView().getLayerManager().redrawLayers();
            }
        } else {
            getActivity().runOnUiThread(() -> register(layer));
        }
    }

    protected void unregister(final Layer layer){
        if (layer == null) return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
//            mgLog.d("unregister fs="+this.getClass().getSimpleName()+" layer="+((layer==null)?"":layer.getClass().getSimpleName())+" control="+(layer instanceof ControlMVLayer));
            synchronized (getMapView().getLayerManager().getLayers()) {
                if (layer instanceof ControlMVLayer){
                    fsControlLayers.layers.remove(layer);
                } else {
                    fsLayers.layers.remove(layer);
                }
                getMapView().getLayerManager().redrawLayers();
            }
        } else {
            getActivity().runOnUiThread(() -> unregister(layer));
        }
    }

    protected void unregisterAll(){
        if (!fsLayers.layers.isEmpty()){
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mgLog.d("unregister "+this);
                synchronized (getMapView().getLayerManager().getLayers()) {
                    fsLayers.layers.clear();
                    getMapView().getLayerManager().redrawLayers();
                }
            } else {
                getActivity().runOnUiThread(this::unregisterAll);
            }
        }
    }
    protected void unregisterAllControl(){
        if (!fsControlLayers.layers.isEmpty()){
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                mgLog.d("unregister "+this);
                synchronized (getMapView().getLayerManager().getLayers()){
                    fsControlLayers.layers.clear();
                    getMapView().getLayerManager().redrawLayers();
                }
            } else {
                getActivity().runOnUiThread(this::unregisterAllControl);
            }
        }
    }

    protected void redraw(){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
//            mgLog.d("redraw fs="+this.getClass().getSimpleName());
            synchronized (getMapView().getLayerManager().getLayers()) {
                getMapView().getLayerManager().redrawLayers();
            }
        } else {
            getActivity().runOnUiThread(this::redraw);
        }
    }

    public MGMapActivity getActivity(){
        return activity;
    }
    public MGMapApplication getApplication(){
        return (MGMapApplication) activity.getApplication();
    }

    protected PersistenceManager getPersistenceManager() {
        return getApplication().getPersistenceManager();
    }
    protected MapView getMapView(){
        return activity.getMapsforgeMapView();
    }
    protected MGMapLayerFactory getMapLayerFactory(){
        return activity.mapLayerFactory;
    }
    protected static Handler getTimer(){
        return timer;
    }
    protected MapViewUtility getMapViewUtility(){
        return activity.getMapViewUtility();
    }
    protected ControlView getControlView(){
        return activity.getControlView();
    }
    protected SharedPreferences getSharedPreferences(){
        return activity.getSharedPreferences();
    }
    protected Resources getResources(){
        return activity.getApplicationContext().getResources();
    }
    protected String r(int id){return getResources().getString(id); }

    protected void onResume(){ }
    protected void onPause(){
        cancelRefresh();
    }
    protected void onDestroy(){ }
    public void onTrimMemory(int level){ }

    protected <T> Pref<T> getPref(int id, T defaultValue){
        return activity.getPrefCache().get(id,defaultValue);
    }
    protected <T> Pref<T> getPref(String key, T defaultValue){
        return activity.getPrefCache().get(key,defaultValue);
    }

    public Drawable getDrawable(int drawableId){
        return ResourcesCompat.getDrawable(getResources(), drawableId, getActivity().getTheme());
    }
}
