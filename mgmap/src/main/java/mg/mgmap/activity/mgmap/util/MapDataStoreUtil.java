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
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.List;

import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.generic.util.WayProvider;

public class MapDataStoreUtil implements WayProvider {

    MGMapLayerFactory mapLayerFactory;

    public MapDataStoreUtil(MGMapLayerFactory mapLayerFactory) {
        this.mapLayerFactory = mapLayerFactory;
    }

    public List<Way> getWays(Tile tile) {
        List<Way> wayList = new ArrayList<>();
            MapReadResult mapReadResult = mapLayerFactory.readMapData(tile);
            if (mapReadResult != null) {
                wayList = mapReadResult.ways;
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
