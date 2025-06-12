package mg.mgmap.generic.graph.implbb;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MemoryUtil;

public class BGraphTileFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());




    private final WriteablePointModel clipRes = new WriteablePointModelImpl();


    private final byte ZOOM_LEVEL = 15;
    private final int TILE_SIZE = MapViewerBase.TILE_SIZE;
    static final int LOW_MEMORY_THRESHOLD = 1;

    static int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }

    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    private boolean wayDetails;
    Pref<String> prefRoutingAlgorithm;
    Pref<Boolean> prefSmooth4Routing;


    public BGraphTileFactory(){}

    public BGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, boolean wayDetails, Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.wayDetails = wayDetails;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.prefSmooth4Routing = prefSmooth4Routing;

        return this;
    }

    // TODO
//    public ArrayList<? extends Graph> getGraphList(BBox bBox){
//        return getBGraphTileList(bBox);
//    }

    public ArrayList<BGraphTile> getBGraphTileList(BBox bBox){
        ArrayList<BGraphTile> tileList = new ArrayList<>();
        try {
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create BGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    BGraphTile graph = getBGraphTile(tileX,tileY);
                    tileList.add(graph);
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return tileList;
    }


    public BGraphTile getBGraphTile(int tileX, int tileY){
        return getBGraphTile(tileX, tileY, true);
    }
    public BGraphTile getBGraphTile(int tileX, int tileY, boolean load){
        BGraphTile bGraphTile = null;
        if (load){
            synchronized (this){  // prevent parallel access from routing thread and FSGraphDetails
                bGraphTile = loadGGraphTile(tileX, tileY);
                if (prefSmooth4Routing.getValue()){
                    bGraphTile.smoothGraph();
                }
            }
        }
        return bGraphTile;
    }


    private BGraphTile loadGGraphTile(int tileX, int tileY){
        Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
        mgLog.i(()->"Load tileX=" + tileX + " tileY=" + tileY + " "+tile.getBoundingBox().getCenterPoint());
        BGraphTile bGraphTile = new BGraphTile(wayProvider, elevationProvider, tile);
        bGraphTile.loadGGraphTile();
        return bGraphTile;
    }


}
