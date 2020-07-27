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
package mg.mapviewer;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.common.Observer;

import java.util.ArrayList;
import java.util.Observable;

import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.util.MapViewUtility;
import mg.mapviewer.view.MultiPointGLView;
import mg.mapviewer.view.MultiPointView;

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
public class MGMicroService {

    private MGMapActivity mmActivity;
    protected ArrayList<Layer> msLayers = new ArrayList<>();

    public MGMicroService(MGMapActivity mmActivity){
        this.mmActivity = mmActivity;
    }

    public class RefreshObserver implements Observer, java.util.Observer{

        @Override
        public void update(Observable o, Object arg) {
            onUpdate(o,arg);
            onChange();
        }

        @Override
        public void onChange() {
            getTimer().removeCallbacks(ttRefresh);
            getTimer().postDelayed(ttRefresh,ttRefreshTime);
        }
    }

    protected void onUpdate(Observable o, Object arg){} //hook for derived classes

    protected long ttRefreshTime = 100;
    private Runnable ttRefresh = new Runnable() {
        @Override
        public void run() {
            doRefresh();
        }
    };
    protected RefreshObserver refreshObserver = new RefreshObserver();

    protected void doRefresh(){}

    protected void showTrack(TrackLog trackLog, Paint paint, boolean showGL){
        showTrack(trackLog,paint,showGL,MultiPointView.POINT_RADIUS);
    }

    protected void showTrack(TrackLog trackLog, Paint paint, boolean showGL, int pointRadius){
        for (int idx = 0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            MultiPointView layer = (showGL)?new MultiPointGLView(segment, paint):new MultiPointView(segment, paint);
            if (showGL) layer.setStrokeIncrease(1.5);
            register(layer, true);
        }
    }

    protected void register(Layer layer){
        register(layer, true);
    }
    protected void register(final Layer layer, final boolean addToMsLayers){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (layer instanceof MVLayer) {
                ((MVLayer) layer).setMapViewUtility(getMapViewUtility());
            }
            if (addToMsLayers) msLayers.add(layer);
            getMapView().getLayerManager().getLayers().add(layer);
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    register(layer, addToMsLayers);
                }
            });
        }
    }
    protected void unregister(Layer layer){
        unregister(layer, true);
    }
    protected void unregister(final Layer layer, final boolean removeFromMsLayers){
        if (layer == null) return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (removeFromMsLayers) msLayers.remove(layer);
            getMapView().getLayerManager().getLayers().remove(layer);
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    unregister(layer, removeFromMsLayers);
                }
            });
        }
    }
    protected <T> int unregisterClass(Class<T> tClass){
        ArrayList<Layer> layers2Bremoved  = new ArrayList<>();
        for (Layer layer : getMapView().getLayerManager().getLayers()){
            if (tClass.isInstance(layer)){
                layers2Bremoved.add(layer);
            }
        }
        for (Layer layer : layers2Bremoved){
            unregister(layer, false);
        }
        return layers2Bremoved.size();
    }

    protected <T>  void unregisterAll(Class<T> tClass){
        for (Layer layer : msLayers){
            if (tClass.isInstance(layer)){
                getMapView().getLayerManager().getLayers().remove(layer);
            }
        }
        msLayers.clear();
    }

    protected void unregisterAll(){
        for (Layer layer : msLayers){
            getMapView().getLayerManager().getLayers().remove(layer);
        }
        msLayers.clear();
    }

    protected MGMapActivity getActivity(){
        return mmActivity;
    }
    protected MGMapApplication getApplication(){
        return (MGMapApplication)mmActivity.getApplication();
    }

    protected MapView getMapView(){
        return mmActivity.getMapsforgeMapView();
    }
    protected Handler getTimer(){
        return mmActivity.getTimer();
    }
    protected MapViewUtility getMapViewUtility(){
        return mmActivity.getMapViewUtility();
    }
    protected ControlView getControlView(){
        return mmActivity.getControlView();
    }
    protected SharedPreferences getSharedPreferences(){
        return mmActivity.getSharedPreferences();
    }
    protected Resources getResources(){
        return mmActivity.getApplicationContext().getResources();
    }


    protected void start(){ }
    protected void stop(){ }

}
