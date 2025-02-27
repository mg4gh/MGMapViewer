/*
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015-2022 devemux86
 * Copyright 2024 Sublimis
 * Copyright 2024 mg4gh
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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;

//import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

//import mg.mgmap.generic.util.basic.MGLog;

/**
 * A MapDatabase that reads and combines data from multiple map files.
 * The MultiMapDatabase supports the following modes for reading from multiple files:
 * <p>
 * - RETURN_FIRST: The data from the first database to support a tile will be returned. This is the
 * fastest operation suitable when you know there is no overlap between map files.
 * <p>
 * - RETURN_ALL: The data from all files will be returned, the data will be combined. This is suitable
 * if more than one file can contain data for a tile, but you know there is no semantic overlap, e.g.
 * one file contains contour lines, another road data. Use {@link #setPriority(int)} to prioritize your maps.
 * <p>
 * - DEDUPLICATE: The data from all files will be returned but duplicates will be eliminated. This is
 * suitable when multiple maps cover the different areas, but there is some overlap at boundaries. This
 * is the most expensive operation and often it is actually faster to double paint objects.
 * Use {@link #setPriority(int)} to prioritize your maps.
 */
public class MultiMapDataStore extends MapDataStore {

//    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public enum DataPolicy {
        RETURN_FIRST, // return the first set of data
        RETURN_ALL, // return all data from databases
        DEDUPLICATE, // return all data but eliminate duplicates
        FAST_DETAILS // if there is a mapsforge map with inner border (as from openandromaps), then you can assume that a fully supported tile contains all ways - so no need to
                     // merge with another one; if there are two such maps, take the younger one.
    }

    private BoundingBox boundingBox;
    private final DataPolicy dataPolicy;
    private final List<MapDataStore> mapDatabases;
    private LatLong startPosition;
    private byte startZoomLevel;

    public MultiMapDataStore(DataPolicy dataPolicy) {
        this.dataPolicy = dataPolicy;
        this.mapDatabases = new ArrayList<>();
    }

    /**
     * adds another mapDataStore
     *
     * @param mapDataStore      the mapDataStore to add
     * @param useStartZoomLevel if true, use the start zoom level of this mapDataStore as the start zoom level
     * @param useStartPosition  if true, use the start position of this mapDataStore as the start position
     */

    public void addMapDataStore(MapDataStore mapDataStore, boolean useStartZoomLevel, boolean useStartPosition) {
        if (this.mapDatabases.contains(mapDataStore)) {
            throw new IllegalArgumentException("Duplicate map database");
        }
        this.mapDatabases.add(mapDataStore);
        if (useStartZoomLevel) {
            setStartZoomLevel(mapDataStore.startZoomLevel());
        }
        if (useStartPosition) {
            setStartPosition(mapDataStore.startPosition());
        }
        if (null == this.boundingBox) {
            this.boundingBox = mapDataStore.boundingBox();
        } else {
            this.boundingBox = this.boundingBox.extendBoundingBox(mapDataStore.boundingBox());
        }

        this.mapDatabases.sort((mds1, mds2) -> {
            // Reverse order
            return -Integer.compare(mds1.getPriority(), mds2.getPriority());
        });
    }

    @Override
    public BoundingBox boundingBox() {
        return this.boundingBox;
    }

    @Override
    public void close() {
        for (MapDataStore mdb : mapDatabases) {
            mdb.close();
        }
    }

    /**
     * Returns the timestamp of the data used to render a specific tile.
     * <p/>
     * If the tile uses data from multiple data stores, the most recent timestamp is returned.
     *
     * @param tile A tile.
     * @return the timestamp of the data used to render the tile
     */
    @Override
    public long getDataTimestamp(Tile tile) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        yield mdb.getDataTimestamp(tile);
                    }
                }
                yield 0;
            }
            case FAST_DETAILS, RETURN_ALL, DEDUPLICATE -> {
                long result = 0;
                for (MapDataStore mdb : mapDatabases) {
                    if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                        if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(tile)){
                            result = extendedMapFile.getDataTimestamp(tile);
                            break;
                        }
                    }
                    if (mdb.supportsTile(tile)) {
                        result = Math.max(result, mdb.getDataTimestamp(tile));
                    }
                }
                yield result;
            }
        };
    }

    @Override
    public MapReadResult readNamedItems(Tile tile) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        yield mdb.readNamedItems(tile);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllLabels(tile);
            case FAST_DETAILS, DEDUPLICATE -> readAllLabels(tile).deduplicate();
        };

    }

    private MapReadResult readAllLabels(Tile tile) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(tile)){
                    mapReadResult = extendedMapFile.readNamedItems(tile);
                    break;
                }
            }
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsTile(tile)) {
                MapReadResult result = mdb.readNamedItems(tile);
                if (result == null) {
                    continue;
                }
                mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullTile(tile)) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readNamedItems(Tile upperLeft, Tile lowerRight) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel
                    )) {
                        yield mdb.readNamedItems(upperLeft, lowerRight);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllLabels(upperLeft, lowerRight);
            case FAST_DETAILS, DEDUPLICATE -> readAllLabels(upperLeft, lowerRight).deduplicate();
        };

    }

    private MapReadResult readAllLabels(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(upperLeft, lowerRight)){
                    mapReadResult = extendedMapFile.readNamedItems(upperLeft, lowerRight);
                    break;
                }
            }
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel
            )) {
                MapReadResult result = mdb.readNamedItems(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readMapData(Tile tile) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        yield mdb.readMapData(tile);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllMapData(tile);
            case FAST_DETAILS, DEDUPLICATE -> readAllMapData(tile).deduplicate();
        };
    }

    private MapReadResult readAllMapData(Tile tile) {
//        mgLog.v(  " x="+tile.tileX+" y="+tile.tileY+" z="+tile.zoomLevel+ " "+tile.getBoundingBox());
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        if (this.dataPolicy==DataPolicy.FAST_DETAILS){
            for (MapDataStore mdb : mapDatabases) {                                                 // if there is a mapDataStore
                if (mdb instanceof ExtendedMapFile extendedMapFile){
                    if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(tile)){ // with inner border that covers the tile fully
                        mapReadResult = extendedMapFile.readMapData(tile);                          // then take just the result from this store
                        isTileFilled = true;                                                        // and stop further processing
//                        mgLog.v(  "id="+extendedMapFile.getId()+" mapReadResult.ways.size="+mapReadResult.ways.size());
                        break;
                    }
                }
            }
        }
        if (!isTileFilled){
            for (MapDataStore mdb : mapDatabases) {
                if (isTileFilled && mdb.getPriority() < 0) {
                    break;
                }

                if (mdb.supportsTile(tile)) {
                    MapReadResult result = mdb.readMapData(tile);
                    if (result == null) {
                        continue;
                    }
                    mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                    mapReadResult.add(result);
                }

                if (mdb.supportsFullTile(tile)) {
                    isTileFilled = true;
                }
            }

        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readMapData(Tile upperLeft, Tile lowerRight) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel
                    )) {
                        yield mdb.readMapData(upperLeft, lowerRight);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllMapData(upperLeft, lowerRight);
            case FAST_DETAILS, DEDUPLICATE -> readAllMapData(upperLeft, lowerRight).deduplicate();
        };
    }

    private MapReadResult readAllMapData(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(upperLeft, lowerRight)){
                    mapReadResult = extendedMapFile.readMapData(upperLeft, lowerRight);
                    break;
                }
            }
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                MapReadResult result = mdb.readMapData(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readPoiData(Tile tile) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsTile(tile)) {
                        yield mdb.readPoiData(tile);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllPoiData(tile);
            case FAST_DETAILS, DEDUPLICATE -> readAllPoiData(tile).deduplicate();
        };

    }

    private MapReadResult readAllPoiData(Tile tile) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(tile)){
                    mapReadResult = extendedMapFile.readMapData(tile);
                    break;
                }
            }
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsTile(tile)) {
                MapReadResult result = mdb.readPoiData(tile);
                if (result == null) {
                    continue;
                }
                mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullTile(tile)) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    @Override
    public MapReadResult readPoiData(Tile upperLeft, Tile lowerRight) {
        return switch (this.dataPolicy) {
            case RETURN_FIRST -> {
                for (MapDataStore mdb : mapDatabases) {
                    if (mdb.supportsArea(
                            upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                            upperLeft.zoomLevel)
                    ) {
                        yield mdb.readPoiData(upperLeft, lowerRight);
                    }
                }
                yield null;
            }
            case RETURN_ALL -> readAllPoiData(upperLeft, lowerRight);
            case FAST_DETAILS, DEDUPLICATE -> readAllPoiData(upperLeft, lowerRight).deduplicate();
        };

    }

    private MapReadResult readAllPoiData(Tile upperLeft, Tile lowerRight) {
        MapReadResult mapReadResult = new MapReadResult();
        boolean isTileFilled = false;
        for (MapDataStore mdb : mapDatabases) {
            if (this.dataPolicy==DataPolicy.FAST_DETAILS && mdb instanceof ExtendedMapFile extendedMapFile){
                if (extendedMapFile.hasInnerBorder() && extendedMapFile.isInInnerBorder(upperLeft, lowerRight)){
                    mapReadResult = extendedMapFile.readPoiData(upperLeft, lowerRight);
                    break;
                }
            }
            if (isTileFilled && mdb.getPriority() < 0) {
                break;
            }

            if (mdb.supportsArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                MapReadResult result = mdb.readPoiData(upperLeft, lowerRight);
                if (result == null) {
                    continue;
                }
                mapReadResult.isWater = mapReadResult.isWater & result.isWater;
                mapReadResult.add(result);
            }

            if (mdb.supportsFullArea(
                    upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox()),
                    upperLeft.zoomLevel)
            ) {
                isTileFilled = true;
            }
        }
        return mapReadResult;
    }

    public void setStartPosition(LatLong startPosition) {
        this.startPosition = startPosition;
    }

    public void setStartZoomLevel(byte startZoomLevel) {
        this.startZoomLevel = startZoomLevel;
    }

    @Override
    public LatLong startPosition() {
        if (null != this.startPosition) {
            return this.startPosition;
        }
        if (null != this.boundingBox) {
            return this.boundingBox.getCenterPoint();
        }
        return null;
    }

    @Override
    public Byte startZoomLevel() {
        return startZoomLevel;
    }

    @Override
    public boolean supportsTile(Tile tile) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsTile(tile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsFullTile(Tile tile) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsFullTile(tile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsArea(BoundingBox boundingBox, byte zoomLevel) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsArea(boundingBox, zoomLevel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsFullArea(BoundingBox boundingBox, byte zoomLevel) {
        for (MapDataStore mdb : mapDatabases) {
            if (mdb.supportsFullArea(boundingBox, zoomLevel)) {
                return true;
            }
        }
        return false;
    }

    public List<MapDataStore> getMapDatabases() {
        return mapDatabases;
    }

    public DataPolicy getDataPolicy() {
        return dataPolicy;
    }
}
