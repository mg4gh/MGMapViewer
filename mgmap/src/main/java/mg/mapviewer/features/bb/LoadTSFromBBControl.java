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
package mg.mapviewer.features.bb;

import android.util.Log;
import android.view.View;

import org.mapsforge.map.layer.Layer;

import java.util.ArrayList;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMapLayerFactory;
import mg.mapviewer.R;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.features.tilestore.TileStoreLoader;
import mg.mapviewer.features.tilestore.MGTileStore;
import mg.mapviewer.features.tilestore.MGTileStoreLayer;

public class LoadTSFromBBControl extends Control {

    MGMapActivity activity;
    MGMapApplication application;
    FSBB fsbb;
    ArrayList<MGTileStore> tss = null;
    private boolean bAll; // load all tiles of bb or only remaining tiles (which are not yet available)
    private boolean bDrop; // delete tiles (instead of load) - default is false


    public LoadTSFromBBControl(MGMapActivity activity, MGMapApplication application, FSBB fsbb, boolean bAll, boolean bDrop){
        super(true);
        this.application = application;
        this.activity = activity;
        this.fsbb = fsbb;
        this.bAll = bAll;
        this.bDrop = bDrop;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        for (MGTileStore ts : tss){
            try {
                TileStoreLoader tileStoreLoader = new TileStoreLoader(activity, application, ts);
                if (bDrop){
                    tileStoreLoader.dropFromBB(fsbb.getBBox());
                } else {
                    tileStoreLoader.loadFromBB(fsbb.getBBox(), bAll);
                }

            } catch (Exception e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            }
        }
    }



    @Override
    public void onPrepare(View v) {
        v.setEnabled(false);
        if (fsbb.isLoadAllowed()){
            tss = identifyTS();
            if (tss.size() > 0){
                v.setEnabled(true);
            }
        }
        if (bDrop){
            setText(v, controlView.rstring(R.string.btDropTSFromBB) );
        } else {
            setText(v, controlView.rstring(bAll?R.string.btLoadTSFromBBAll:R.string.btLoadTSFromBB) );
        }
    }

    public static ArrayList<MGTileStore> identifyTS(){
        ArrayList<MGTileStore> tss = new ArrayList<>();
        for (Layer layer : MGMapLayerFactory.mapLayers.values()){
            if (layer instanceof MGTileStoreLayer) {
                MGTileStoreLayer mgTileStoreLayer = (MGTileStoreLayer) layer;
                MGTileStore mgTileStore = mgTileStoreLayer.getMGTileStore();
                if (mgTileStore.hasConfig()){
                    tss.add(mgTileStore);
                }
            }
        }
        return tss;
    }
}
