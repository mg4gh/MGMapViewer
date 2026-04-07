package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class MapTest {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @Test
    public void _00_test() {

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());
        MapDataStore mds = new MapFile(mapFile, "de");

        BBox bBox = new BBox().extend( new PointModelImpl(54.422888,13.448283) );
        mgLog.d("bBox="+bBox);

        {
            final byte ZOOM_LEVEL = 15;
            final int TILE_SIZE = MapViewerBase.TILE_SIZE;
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE) & 0xFFFFFFFE;
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE) | 0x00000001;
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE) & 0xFFFFFFFE; // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE) | 0x00000001;

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create GGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);
            long nanosStart = System.nanoTime();
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
                    MapReadResult result = mds.readMapData(tile);
                    mgLog.d("tileX="+tileX+" tileY="+tileY+" numWays="+result.ways.size());
                }
            }
            mgLog.d("time="+(System.nanoTime()-nanosStart)/1_000_000);

        }
        {
            final byte ZOOM_LEVEL = 14;
            final int TILE_SIZE = MapViewerBase.TILE_SIZE;
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create GGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);

            long nanosStart = System.nanoTime();
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
                    MapReadResult result = mds.readMapData(tile);
                    mgLog.d("tileX="+tileX+" tileY="+tileY+" numWays="+result.ways.size());
                }
            }
            mgLog.d("time="+(System.nanoTime()-nanosStart)/1_000_000);

        }


    }

}
