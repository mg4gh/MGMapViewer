package mg.mgmap.generic.graph.implbb;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BGraphTileFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final int MD_TILE_SIZE = 10986;

    private int maxPoints = 8192; // per tile
    private int pointSize =
                    4   // latitude in md (int)
                    + 4 // longitude in md (int)
                    + 4 // elevation as float
                    + 2 // 1 byte (4) flags for border node  + 1 byte (3) flags for height smoothing
                    + 2; // first neighbour index
    private byte[] baPoints = new byte[pointSize*maxPoints];
    ByteBuffer bbPoints = ByteBuffer.wrap(baPoints);

    private int maxNeighbours = 8192; // per tile
    private int neighbourSize =
                    2   // neighbourPointIndex (3 bit tile selector; 13 bit point index) (as short)
                    + 2 // next neighbour index
                    + 4 // distance as float
                    + 4 // cost as a float
                    + 2; // way attributes index
    private byte[] baNeighbours = new byte[neighbourSize*maxNeighbours];
    ByteBuffer bbNeighbours = ByteBuffer.wrap(baNeighbours);

    private int maxWayAttributes = 8192;
    private WayAttributs[] wayAttributes = new WayAttributs[maxWayAttributes];

    int pointsPerLat = 10; // per Lat value there are up to this number of indexes here stored
    short[][] ptIdxPerLat = new short[MD_TILE_SIZE+1][pointsPerLat];
    int[] numPtIdxPerLat = new int[MD_TILE_SIZE+1]; // number of entries in ptIdxPerLat with latIdx (which is lat - bBox.minLat)

    private int maxOverflowPoints = 1024;
    short[] overflowPoints = new short[maxOverflowPoints];

    short pointsUsed = 0;
    short neighboursUsed = 1;
    short wayAttributesUsed = 0;
    short overflowPointsUsed = 0;

    // temporary data for setup of bbPoints
//    int[] latMdCnt = new int[MD_TILE_SIZE+1]; // number of entries with latIdx (which is lat - bBox.minLat)
//    int pointsPerLat = 10; // per Lat value there are up to this number of indexes here stored
//    int pointsPerLatSize = pointsPerLat * Short.SIZE/8;
//    byte[] baLatPtIndexes = new byte[(MD_TILE_SIZE+1)*pointsPerLatSize];
//    ByteBuffer bbLatPtIndexes = ByteBuffer.wrap(baLatPtIndexes);




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



    private BGraphTile loadGGraphTile(int tileX, int tileY){
        mgLog.d(()->"Load tileX=" + tileX + " tileY=" + tileY);
        Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
        BGraphTile bGraphTile = new BGraphTile(elevationProvider, tile);
        // reinit counters for setup
        pointsUsed = 0;
        neighboursUsed = 0;
        wayAttributesUsed = 0;
        overflowPointsUsed = 0;
        for (int i=0; i<MD_TILE_SIZE+1; i++){
            numPtIdxPerLat[i] = 0; //
        }

        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                short waIdx = wayAttributesUsed++;
                wayAttributes[waIdx] = new WayAttributs(way);
                addLatLongs( bGraphTile, waIdx, way.latLongs[0]);

//                // now setup rawWays
//                if (wayDetails){
//                    MultiPointModelImpl mpm = new MultiPointModelImpl();
//                    for (LatLong latLong : way.latLongs[0] ){
//                        // for points inside the tile use the GNodes as already allocated
//                        // for points outside use extra Objects, don't pollute the graph with them
//                        if (gGraphTile.bBox.contains(latLong.latitude, latLong.longitude)){
//                            mpm.addPoint(gGraphTile.getAddNode(latLong.latitude, latLong.longitude));
//                        } else {
//                            mpm.addPoint(new PointModelImpl(latLong));
//                        }
//                    }
//                    gGraphTile.getRawWays().add(mpm);
//                }
            }
        }
//        int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(GGraph.CONNECT_THRESHOLD_METER) );
//        int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(GGraph.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
////            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
//        //all highways are in the map ... try to correct data ...
//        ArrayList<GNode> nodes = gGraphTile.getGNodes();
//        for (int iIdx=0; iIdx<nodes.size(); iIdx++){
//            GNode iNode = nodes.get(iIdx);
//            int iNeighbours = gGraphTile.countNeighbours(iNode);
//            for (int nIdx=iIdx+1; nIdx<nodes.size(); nIdx++ ) {
//                GNode nNode = nodes.get(nIdx);
//                if (iNode.laMdDiff(nNode) >= latThreshold) break; // go to next iIdx
//                if (iNode.loMdDiff(nNode) >= lonThreshold)
//                    continue; // goto next mIdx
//                if (PointModelUtil.distance(iNode, nNode) > GGraph.CONNECT_THRESHOLD_METER)
//                    continue;
//                if (gGraphTile.getNeighbour(iNode,nNode)!=null)
//                    continue; // is already neighbour
//
////This doesn't work well for routing hints
////                    graph.addSegment(iNode, nNode);
//
////And this didn't work too - removes the resulting point from tile clip process
////                  // Try to simplify the graph by removing node nNode
//                // iterate over al neighbours from nNode
////                    GNeighbour nextNeighbour = nNode.getNeighbour();
////                    while (nextNeighbour.getNextNeighbour() != null) {
////                        nextNeighbour = nextNeighbour.getNextNeighbour();
////                        // remove nNode as a Neighbour
////                        nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
////                        graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
////                    }
//
////And this is still not good: (Hollmuth,Heiligkreuzsteinach) there are 2 neighbours at one end ... and one on the other ... and this doesn't work
////                    if (nNode.countNeighbours() != 1) continue; // connect only end points
////                    // Third solution approach: connect only point with exactly 1 neighbour
////                    // Therefore this shouldn't be a Problem for routing hints, since both connected points have now 2 neighbours - so they are no routing points
////                    graph.addSegment(iNode, nNode);
//
//                int nNeighbours = gGraphTile.countNeighbours(nNode);
//                if ((iNeighbours == 0) || (nNeighbours == 0)) { // don't connect, if a node has no neighbours (might occur due to former reduceGraph action)
//                    continue;
//                }
//                if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
//                    gGraphTile.addSegment(null,iNode, nNode);
//                    continue;
//                }
//                if (isBorderPoint(gGraphTile.bBox, nNode) || isBorderPoint(gGraphTile.bBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
//                    gGraphTile.addSegment(null,iNode, nNode);
//                    continue;
//                }
//                if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
//                    reduceGraph(gGraphTile, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
//                    continue;
//                }
//                if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
//                    reduceGraph(gGraphTile, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
//                    iNeighbours = 0; // just in case there is a second close nNode
//                    continue;
//                }
//                // else (n:m) accept routing hint issue
//                gGraphTile.addSegment(null,iNode, nNode);
//
//            }
//        }
        return bGraphTile;
    }

    void addLatLongs(BGraphTile bGraphTile, short wayAttributesIdx, LatLong[] latLongs){
        for (int i=1; i<latLongs.length; i++){
            double lat1 = PointModelUtil.roundMD(latLongs[i-1].latitude);
            double lon1 = PointModelUtil.roundMD(latLongs[i-1].longitude);
            double lat2 = PointModelUtil.roundMD(latLongs[i].latitude);
            double lon2 = PointModelUtil.roundMD(latLongs[i].longitude);

            BBox bBox = bGraphTile.bBox;
            bGraphTile.bBox.clip(lat1, lon1, lat2, lon2, clipRes); // clipRes contains the clip result
            lat2 = clipRes.getLat();
            lon2 = clipRes.getLon();
            bBox.clip(lat2, lon2, lat1, lon1, clipRes); // clipRes contains the clip result
            lat1 = clipRes.getLat();
            lon1 = clipRes.getLon();
            if (bBox.contains(lat1, lon1) && bBox.contains(lat2, lon2)){
                addSegment(LaLo.d2md(bBox.minLatitude), wayAttributesIdx, LaLo.d2md(lat1), LaLo.d2md(lon1) ,LaLo.d2md(lat2), LaLo.d2md(lon2));
            }
        }
    }

    void addSegment(int minLat, short wayAttributesIdx, int lat1, int lon1, int lat2, int lon2){
        short pt1Idx = getAddPoint(minLat, lat1, lon1);
        short pt2Idx = getAddPoint(minLat, lat2, lon2);
        addSegment(wayAttributesIdx, pt1Idx, pt2Idx);
    }

    void addSegment(short wayAttributesIdx, short pt1Idx, short pt2Idx) {
        bbPoints.position(pt1Idx*pointSize);
        double dLat1 = LaLo.md2d(bbPoints.getInt());
        double dLon1 = LaLo.md2d(bbPoints.getInt());
        bbPoints.position(pt2Idx*pointSize);
        double dLat2 = LaLo.md2d(bbPoints.getInt());
        double dLon2 = LaLo.md2d(bbPoints.getInt());
        double distance = PointModelUtil.distance(dLat1, dLon1, dLat2, dLon2);
        short n12Idx = createNeighbour(wayAttributesIdx, pt2Idx, (float) distance);
        short n21Idx = createNeighbour(wayAttributesIdx, pt1Idx, (float) distance);
        addNeighbour(pt1Idx, n12Idx);
        addNeighbour(pt2Idx, n21Idx);
    }

    private int lastLat = LaLo.d2md(PointModel.NO_LAT_LONG);
    private int lastLon = LaLo.d2md(PointModel.NO_LAT_LONG);
    private short lastIdx = -1;
    // get point index for given lat+lon; if such point is not yet registered, add this point and return its index
    private short getAddPoint(int minLat, int lat, int lon){
        if ((lat == lastLat) && (lon == lastLon)) return lastIdx;
        short ptIdx = -1;
        int latIdx = lat - minLat;
        for (int i=0; i<numPtIdxPerLat[latIdx]; i++){
            short pIdx = ptIdxPerLat[latIdx][i];
            bbPoints.position( pIdx * pointSize + 4);
            if (bbPoints.getInt() == lon){
                ptIdx = pIdx;
                break;
            }
        }
        if ((ptIdx < 0) && (numPtIdxPerLat[latIdx] == pointsPerLat)){ // point not yet found and row is full, check also overflow area
            for (int i=0; i<overflowPointsUsed; i++){
                short pIdx = overflowPoints[i];
                bbPoints.position( pIdx * pointSize);
                int aLat = bbPoints.getInt();
                int aLon = bbPoints.getInt();
                if ((aLon == lon) && (aLat == lat)){
                    return pIdx;
                }
            }
        }
        if (ptIdx < 0){ // point still not found -> create it
            ptIdx = pointsUsed++;
            bbPoints.position(ptIdx*pointSize);
            bbPoints.putInt(lat);
            bbPoints.putInt(lon);

            if (numPtIdxPerLat[latIdx] == pointsPerLat){ // row is full, put point to overflow area
                overflowPoints[overflowPointsUsed++] = ptIdx;
            } else {
                ptIdxPerLat[latIdx][ numPtIdxPerLat[latIdx] ] = ptIdx;
                numPtIdxPerLat[latIdx]++;
            }
        }
        lastLat = lat;
        lastLon = lon;
        lastIdx = ptIdx;
        return ptIdx;
    }

    public short createNeighbour(short wayAttributesIdx, short neighbourPointIdx, float distance){
        short nIdx = neighboursUsed++;
        bbNeighbours.position(neighbourSize*nIdx);
        bbNeighbours.putShort(neighbourPointIdx);
        bbNeighbours.putShort((short)0); // next neighbour index
        bbNeighbours.putFloat(distance);
        bbNeighbours.putFloat(0); // cost
        bbNeighbours.putShort(wayAttributesIdx);
        return nIdx;
    }

    public void addNeighbour(short pointIdx, short neighbourIdx){
        bbPoints.position(pointIdx*pointSize + 14); // 14 is neighbour offset
        short nIdx = bbPoints.getShort();
        short nnIdx;
        while ((nnIdx = getNextNeighbour(nIdx)) != 0){
            nIdx = nnIdx;
        }
        setNextNeighbour(nIdx, neighbourIdx);
    }

    public short getNextNeighbour(short nIdx){
        bbNeighbours.position(nIdx*neighbourSize + 2); // 2 next neighbour offset
        return bbNeighbours.getShort();
    }
    public void setNextNeighbour(short nIdx, short nnIdx){
        bbNeighbours.position(nIdx*neighbourSize + 2); // 2 next neighbour offset
        bbNeighbours.putShort(nnIdx);
    }


//    private int neighbourSize =
//            2   // neighbourPointIndex (3 bit tile selector; 13 bit point index) (as short)
//                    + 2 // next neighbour index
//                    + 4 // distance as float
//                    + 4 // cost as a float
//                    + 2; // way attributes index

}
