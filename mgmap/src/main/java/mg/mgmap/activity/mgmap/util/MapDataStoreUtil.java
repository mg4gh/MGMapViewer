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

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.activity.mgmap.MultiMapDataStore;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.util.WayProvider;

public class MapDataStoreUtil implements WayProvider {

    private boolean active;
    private TreeMap<MapDataStore, String> mapDataStoreMap;
    private final MultiMapDataStore mmds = new MultiMapDataStore(MultiMapDataStore.DataPolicy.FAST_DETAILS);

    public MapDataStoreUtil() {
        mapDataStoreMap = new TreeMap<>((mds1, mds2) -> {
            int res = -Integer.compare(mds1.getPriority(), mds2.getPriority());
            if (res == 0) {
                res = Long.compare(BBox.fromBoundingBox(mds1.boundingBox()).fingerprint(), BBox.fromBoundingBox(mds2.boundingBox()).fingerprint());
            }
            return res;
        });
        active = true;
    }

    public void register(String id, MapDataStore mds) {
        mapDataStoreMap.put(mds, id);
        mmds.addMapDataStore(mds, false, false);
    }

    public Map<MapDataStore, String> getMapDataStoreMap() {
        return mapDataStoreMap;
    }

    public void onDestroy() {
        mapDataStoreMap = null;
        mmds.close();
        active = false;
    }

    public MapDataStore getMapDataStore(BBox bBox) {
        for (MapDataStore mds : mapDataStoreMap.keySet()) {
            if (BBox.fromBoundingBox(mds.boundingBox()).contains(bBox)) return mds;
        }
        return null;
    }

    public List<Way> getWays(Tile tile) {
        List<Way> wayList = new ArrayList<>();
        if (active) {
            MapReadResult mapReadResult = mmds.readMapData(tile);
            if (mapReadResult != null) {
                wayList = mapReadResult.ways;
            }
        }
        return wayList;
    }

    public boolean isWayForRouting(Way way) {
        for (Tag tag : way.tags) {
            if (tag.key.equals("highway")) {
                return true;
            }
            if (tag.key.equals("route") && tag.value.equals("ferry")) {
                return true;
            }
        }
        return false;
    }

}
