package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Locale;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class TileTest {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final byte ZOOM_LEVEL = 15;
    private final int MD_TILE_SIZE = 10986;
    private final int TILE_SIZE = MapViewerBase.TILE_SIZE;

    @Test
    public void _01_Tiles() {
        MGLog.setUnittest(true);

        BBox bBox = new BBox().extend(0,0).extend(49.5, 8.5);
        try {
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int minWidth = Integer.MAX_VALUE;
            int maxWidth = Integer.MIN_VALUE;
            int minHeight = Integer.MAX_VALUE;
            int maxHeight = Integer.MIN_VALUE;
            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.i(()->"check tiles with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
                    BoundingBox bb = tile.getBoundingBox();
                    int width = LaLo.d2md(bb.maxLongitude - bb.minLongitude);
                    int height = LaLo.d2md(bb.maxLatitude - bb.minLatitude);
                    if (width < minWidth) minWidth = width;
                    if (width > maxWidth) maxWidth = width;
                    if (height < minHeight) minHeight = height;
                    if (height > maxHeight) maxHeight = height;
                }
            }
            mgLog.i(String.format(Locale.ENGLISH, "minWidth=%d minHeight=%d   maxWidth=%d maxHeight=%d",minWidth, minHeight, maxWidth, maxHeight));

        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    @Test
    public void _02_Tiles() {
        MGLog.setUnittest(true);

        BBox bBox = new BBox().extend(40,5).extend(60, 16.1);
        try {
            File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
            System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

            MapDataStore mds = new MapFile(mapFile, "de");
            WayProvider wayProvider = new WayProviderHelper(mds);

            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int minWidth = Integer.MAX_VALUE;
            int maxWidth = Integer.MIN_VALUE;
            int minHeight = Integer.MAX_VALUE;
            int maxHeight = Integer.MIN_VALUE;
            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.i(()->"check tiles with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);
            int maxLonCnt = Integer.MIN_VALUE;
            int maxLatCnt = Integer.MIN_VALUE;
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
                    BoundingBox boundingBox = tile.getBoundingBox();
                    int minLon = LaLo.d2md(boundingBox.minLongitude);
                    int minLat = LaLo.d2md(boundingBox.minLatitude);


                    int[] lonMdCnt = new int[MD_TILE_SIZE+1];
                    int[] latMdCnt = new int[MD_TILE_SIZE+1];
                    int rowSize = 200;
                    byte[] lats = new byte[(MD_TILE_SIZE+1)*rowSize];
                    ByteBuffer bbLats = ByteBuffer.wrap(lats);
                    byte[] lons = new byte[(MD_TILE_SIZE+1)*rowSize];
                    ByteBuffer bbLons = ByteBuffer.wrap(lons);

                    for (Way way : wayProvider.getWays(tile)) {
                        if (wayProvider.isWayForRouting(way)) {
                            for (LatLong latLong : way.latLongs[0]){
                                if (boundingBox.contains(latLong)){
                                    int latIdx = LaLo.d2md(latLong.latitude) - minLat;
                                    int lonIdx = LaLo.d2md(latLong.longitude) - minLon;
                                    bbLats.position(latIdx*rowSize);
                                    boolean lonIdxExists = false;
                                    for (int i=0; i<latMdCnt[latIdx];i++){
                                        int aLonIdx = bbLats.getInt();
                                        if (aLonIdx == lonIdx) {
                                            lonIdxExists=true;
                                            break;
                                        }
                                    }
                                    bbLons.position(lonIdx*rowSize);
                                    boolean latIdxExists = false;
                                    for (int i=0; i<lonMdCnt[lonIdx];i++){
                                        int aLatIdx = bbLons.getInt();
                                        if (aLatIdx == latIdx) {
                                            latIdxExists=true;
                                            break;
                                        }
                                    }
                                    assert(latIdxExists == lonIdxExists);
                                    if (!lonIdxExists){
                                        bbLats.putInt(lonIdx);
                                        latMdCnt[latIdx]++;
                                    }
                                    if (!latIdxExists){
                                        bbLons.putInt(latIdx);
                                        lonMdCnt[lonIdx]++;
                                    }

                                }
                            }
                        }
                    }
                    int rowEntries = 10;

                    for (int ii=0; ii<MD_TILE_SIZE+1; ii++){
                        if (latMdCnt[ii] > rowEntries){
                            bbLats.position(ii*rowSize);
                            for (int i=0; i<latMdCnt[ii];i++){
                                int aLonIdx = bbLats.getInt();
                                if (lonMdCnt[aLonIdx] > rowEntries){
                                    mgLog.i("tileX="+tileX+" tileY="+tileY+" "+latMdCnt[ii]+" "+lonMdCnt[aLonIdx]);
                                }
                            }
                        }



                        if (lonMdCnt[ii] > maxLonCnt){
                            maxLonCnt = lonMdCnt[ii];
                            mgLog.i("maxLonCnt="+maxLonCnt);
                        }
                        if (latMdCnt[ii] > maxLatCnt){
                            maxLatCnt = latMdCnt[ii];
                            mgLog.i("maxLatCnt="+maxLatCnt);
                        }
                    }

                }
            }
            mgLog.i(String.format(Locale.ENGLISH, "minWidth=%d minHeight=%d   maxWidth=%d maxHeight=%d",minWidth, minHeight, maxWidth, maxHeight));

        } catch (Exception e) {
            mgLog.e(e);
        }
    }


    @Test
    public void _03_Tiles() {
        MGLog.setUnittest(true);

        BBox bBox = new BBox().extend(40,5).extend(60, 16.1);
        try {
            File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
            System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

            int maxPoints = 20000;
            int pointSize = 16;
            short pointsUsed = 0;
            byte[] baPoints = new byte[maxPoints*pointSize];
            ByteBuffer bbPoints = ByteBuffer.wrap(baPoints);


            int[] latMdCnt = new int[MD_TILE_SIZE+1];
            int pointsPerRow = 10;
            int rowSize = pointsPerRow * Short.SIZE/8;
            byte[] lats = new byte[(MD_TILE_SIZE+1)*rowSize];
            ByteBuffer bbLats = ByteBuffer.wrap(lats);

            int overflowUsed = 0;
            short[] overflowPoints = new short[1000];
            long overflowCompare = 0;


            MapDataStore mds = new MapFile(mapFile, "de");
            WayProvider wayProvider = new WayProviderHelper(mds);

            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.i(()->"check tiles with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);

            int overflowUsedMax = Integer.MIN_VALUE;
            long overflowUsedAvg = 0;
            long overflowCompareMax = Long.MIN_VALUE;
            long overflowCompareAvg = 0;

            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
                    BoundingBox boundingBox = tile.getBoundingBox();
                    int minLon = LaLo.d2md(boundingBox.minLongitude);
                    int minLat = LaLo.d2md(boundingBox.minLatitude);


                    pointsUsed = 0;
                    overflowUsed = 0;
                    overflowCompare = 0;
                    for (int i=0; i<MD_TILE_SIZE+1; i++) latMdCnt[i] = 0;

                    for (Way way : wayProvider.getWays(tile)) {
                        if (wayProvider.isWayForRouting(way)) {
                            for (LatLong latLong : way.latLongs[0]){

                                short ptIdx = -1;
                                if (boundingBox.contains(latLong)){
                                    int mdLat = LaLo.d2md(latLong.latitude);
                                    int latIdx = mdLat - minLat;
                                    int mdLon = LaLo.d2md(latLong.longitude);

                                    bbLats.position(latIdx*rowSize);
                                    for (int i=0; i<latMdCnt[latIdx];i++){
                                        short pIdx = bbLats.getShort();
                                        bbPoints.position( pIdx * pointSize + 4);
                                        int aLon = bbPoints.getInt();
                                        if (aLon == mdLon) {
                                            ptIdx = pIdx;
                                            break;
                                        }
                                    }
                                    if (latMdCnt[latIdx] == pointsPerRow){ // row is full, check also overflow area
                                        for (int i=0; i<overflowUsed; i++){
                                            short pIdx = overflowPoints[i];
                                            bbPoints.position( pIdx * pointSize);
                                            int aLat = bbPoints.getInt();
                                            int aLon = bbPoints.getInt();
                                            overflowCompare++;
                                            if ((aLon == mdLon) && (aLat == mdLat)){
                                                ptIdx = pIdx;
                                                break;
                                            }
                                        }
                                    }
                                    // if point exists, then ptIdx is the index


                                    if (ptIdx < 0){ // add, point, if it not yet exists
                                        ptIdx = pointsUsed++;
                                        bbPoints.position(ptIdx*pointSize);
                                        bbPoints.putInt(mdLat);
                                        bbPoints.putInt(mdLon);

                                        if (latMdCnt[latIdx] == pointsPerRow){ // row is full
                                            overflowPoints[overflowUsed++] = ptIdx;
                                        } else {
                                            bbLats.putShort(ptIdx);
                                            latMdCnt[latIdx]++;
                                        }
                                    }


                                }
                            }
                        }
                    }

                    if (overflowUsed > overflowUsedMax){
                        overflowUsedMax = overflowUsed;
                        mgLog.i("overflowUsedMax="+overflowUsedMax);
                    }
                    overflowUsedAvg += overflowUsed;

                    if (overflowCompare > overflowCompareMax){
                        overflowCompareMax = overflowCompare;
                        mgLog.i("overflowCompareMax="+overflowCompareMax);
                    }
                    overflowCompareAvg += overflowCompare;



                }
            }
            mgLog.i(String.format(Locale.ENGLISH, "totalTiles=%d overflowUsed=%d overflowUsedAvg=%.1f   overflowCompare=%d overflowCompareAvg=%.1f",totalTiles,overflowUsed, overflowUsedAvg*1d, overflowCompare, overflowCompareAvg*1d));

        } catch (Exception e) {
            mgLog.e(e);
        }
    }


}
