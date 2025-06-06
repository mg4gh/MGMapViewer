package mg.mgmap.generic.graph.implbb;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;

import java.util.ArrayList;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public class BGraphTile {

    public static final byte BORDER_NODE_WEST  = 0x08;
    public static final byte BORDER_NODE_NORTH = 0x04;
    public static final byte BORDER_NODE_EAST  = 0x02;
    public static final byte BORDER_NODE_SOUTH = 0x01;

    public static final byte FLAG_FIX               = 0x01;
    public static final byte FLAG_VISITED           = 0x02;
    public static final byte FLAG_HEIGHT_RELEVANT   = 0x04;



    final ElevationProvider elevationProvider;

    private final ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    final Tile tile;
    final int tileIdx;
    final BBox bBox;
    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    private final WriteablePointModel hgtTemp = new WriteablePointModelImpl();
    final BGraphTile[] neighbourTiles = new BGraphTile[BORDER_NODE_WEST+1];//use BORDER constants as index, although some entries stay always null



    boolean used = false; // used for cache - do not delete from cache
    long accessTime = 0; // used for cache

    BGraphTile(ElevationProvider elevationProvider, Tile tile){
        this.elevationProvider = elevationProvider;
        this.tile = tile;
        this.tileIdx = BGraphTileFactory.getKey(getTileX(),getTileY());
        bBox = BBox.fromBoundingBox(this.tile.getBoundingBox());
    }

    public int getTileX(){
        return tile.tileX;
    }
    public int getTileY(){
        return tile.tileY;
    }

    void addLatLongs(WayAttributs wayAttributs, LatLong[] latLongs){
        for (int i=1; i<latLongs.length; i++){
            double lat1 = PointModelUtil.roundMD(latLongs[i-1].latitude);
            double lon1 = PointModelUtil.roundMD(latLongs[i-1].longitude);
            double lat2 = PointModelUtil.roundMD(latLongs[i].latitude);
            double lon2 = PointModelUtil.roundMD(latLongs[i].longitude);

            bBox.clip(lat1, lon1, lat2, lon2, clipRes); // clipRes contains the clip result
            lat2 = clipRes.getLat();
            lon2 = clipRes.getLon();
            bBox.clip(lat2, lon2, lat1, lon1, clipRes); // clipRes contains the clip result
            lat1 = clipRes.getLat();
            lon1 = clipRes.getLon();
            if (bBox.contains(lat1, lon1) && bBox.contains(lat2, lon2)){
//                addSegment(wayAttributs, lat1, lon1 ,lat2, lon2);
            }
        }
    }


}
