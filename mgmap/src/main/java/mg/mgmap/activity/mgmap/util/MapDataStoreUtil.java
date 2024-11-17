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
package mg.mgmap.activity.mgmap.util;

import android.content.SharedPreferences;

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.util.WayProvider;

public class MapDataStoreUtil implements WayProvider {

    private boolean active = false;
    private ArrayList<BBox> mapBBoxList = null;
    private ArrayList<MapDataStore> mapDataStoreList = null;
    private HashMap<Tile, MapDataStore> tile2MapDataStore = null;



    public MapDataStoreUtil onCreate(MGMapLayerFactory mapLayerFactory, SharedPreferences sharedPreferences){
        mapBBoxList = new ArrayList<>();
        mapDataStoreList = new ArrayList<>();
        tile2MapDataStore = new HashMap<>();

        for (String prefKey : mapLayerFactory.getMapLayerKeys()){
            String key = sharedPreferences.getString(prefKey, "none");
            sharedPreferences.edit().putString(prefKey,key).apply(); // so if pref was not existing, then default value "none" is now set - this helps to prevent recreate activity on initial set

            Layer layer = mapLayerFactory.getMapLayer(key);
            if (layer instanceof TileRendererLayer) {
                MapDataStore mds = ((TileRendererLayer)layer).getMapDataStore();
                if (mds != null){
                    mapBBoxList.add(BBox.fromBoundingBox(mds.boundingBox()));
                    mapDataStoreList.add(mds);
                }
            }
        }
        active = true;
        return this;
    }

    public void onDestroy(){
        mapBBoxList = null;
        mapDataStoreList = null;
        tile2MapDataStore = null;
        active = false;
    }

    public MapDataStore getMapDataStore(){
        if (!mapDataStoreList.isEmpty()){
            return mapDataStoreList.get(0);
        }
        return null;
    }

    public MapDataStore getMapDataStore(BBox bBox){
        for (int idx=0; idx < mapBBoxList.size(); idx++){
            if (mapBBoxList.get(idx).contains(bBox)){
                return mapDataStoreList.get(idx);
            }
        }
        return null;
    }

    public List<Way> getWays(Tile tile){
        List<Way> wayList = null;
        if (active){
            MapDataStore mds = tile2MapDataStore.get(tile);
            if (mds != null) {
                wayList = mds.readMapData(tile).ways;
            } else { // mds == null -> try to find proper MapDataStore
                for (int idx=0; idx < mapBBoxList.size(); idx++){
                    if (mapBBoxList.get(idx).contains(tile.getBoundingBox())){
                        mds = mapDataStoreList.get(idx);
                        MapReadResult mapReadResult = mds.readMapData(tile);
                        if (wayList == null){
                            wayList = mapReadResult.ways;
                            tile2MapDataStore.put(tile, mds);
                        } else { // there is more than one TileStoreLayer containing this tile - try to identify the better one
                            if (mapReadResult.ways.size() > wayList.size()){ // seems to have more ways -> update
                                wayList = mapReadResult.ways;
                                tile2MapDataStore.put(tile, mds);
                            }
                        }
                    }
                }
            }
        }
        return (wayList==null)?new ArrayList<>():wayList;
    }

    public boolean isHighway(Way way){
        for (Tag tag : way.tags){
            if (tag.key.equals("highway")){
                return true;
            }
        }
        return false;
    }

}
