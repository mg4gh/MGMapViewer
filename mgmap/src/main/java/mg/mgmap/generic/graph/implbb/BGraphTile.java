package mg.mgmap.generic.graph.implbb;

import static mg.mgmap.generic.graph.implbb.BNeighbours.*;
import static mg.mgmap.generic.graph.implbb.BNodes.*;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;

import mg.mgmap.generic.graph.impl.GNeighbour;
import mg.mgmap.generic.graph.impl.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.PointNeighbour;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BGraphTile implements Graph{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m
    private final static int MD_TILE_SIZE = 10986;

    final static int MAX_POINTS = 8192; // per tile

    final static int MAX_NEIGHBOURS = 8192*8; // per tile

    final static int MAX_WAY_ATTRIBUTES = 8192;
    final static WayAttributs[] wayAttributesInit = new WayAttributs[MAX_WAY_ATTRIBUTES];



    final static byte[] baPointsInit = new byte[POINT_SIZE * MAX_POINTS];
    final static ByteBuffer bbPointsInit = ByteBuffer.wrap(baPointsInit);
    final static byte[] baNeighboursInit = new byte[NEIGHBOUR_SIZE * MAX_NEIGHBOURS];
    final static ByteBuffer bbNeighboursInit = ByteBuffer.wrap(baNeighboursInit);

    static ArrayList<BNode> freeNodes = new ArrayList<>();
    static {
        for (int i=0; i<MAX_POINTS; i++){
            freeNodes.add(new BNode());
        }
    }
    static TreeMap<BNode, BNode> tmBNodes = new TreeMap<>();


    final WayProvider wayProvider;
    final ElevationProvider elevationProvider;
    final Tile tile;

    BNodes nodes = new BNodes();
    BNeighbours neighbours = new BNeighbours();
    WayAttributs[] wayAttributes;
    short wayAttributesUsed = 0;


    private final ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    final int tileIdx;
    final BBox bBox;
    int minLat;
    int maxLat;
    int minLon;
    int maxLon;

    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    private final WriteablePointModel hgtTemp = new WriteablePointModelImpl();
    final BGraphTile[] neighbourTiles = new BGraphTile[BORDER_NODE_WEST+1];//use BORDER constants as index, although some entries stay always null
    boolean init = true; // after initial setup this flag is set to false - access to static arrays is only allowed while init flag is set




    boolean used = false; // used for cache - do not delete from cache
    long accessTime = 0; // used for cache

    BGraphTile(WayProvider wayProvider, ElevationProvider elevationProvider, Tile tile){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.tile = tile;

        this.tileIdx = BGraphTileFactory.getKey(getTileX(),getTileY());
        bBox = BBox.fromBoundingBox(this.tile.getBoundingBox());

        minLat = LaLo.d2md(bBox.minLatitude);
        maxLat = LaLo.d2md(bBox.maxLatitude);
        minLon = LaLo.d2md(bBox.minLongitude);
        maxLon = LaLo.d2md(bBox.maxLongitude);

    }

    public int getTileX(){
        return tile.tileX;
    }
    public int getTileY(){
        return tile.tileY;
    }


    // +++++++++++++++++++++++++++++++++++++ Methods used during initialisation ++++++++++++++++++++++++++++++++++****

    @SuppressWarnings("CommentedOutCode")
    synchronized void loadGGraphTile(){ // synchronized to prevent concurrent access to static ByteBuffers
        nodes.init(bbPointsInit);
        nodes.pointsUsed = 0;
        neighbours.init(bbNeighboursInit);
        neighbours.neighboursUsed = 1; // idx 0 is used as "null"
        this.wayAttributes = wayAttributesInit;
        wayAttributesUsed = 0;

        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                short waIdx = wayAttributesUsed++;
                wayAttributes[waIdx] = new WayAttributs(way);

                addLatLongs( waIdx, way.latLongs[0]);

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
        mgLog.i("XXXX optimize BGraph -- wayAttributesUsed="+wayAttributesUsed);
//        short xxxGN = getAddPoint(49300645, 8089221);
//        short xxxGN = getAddPoint(49702800, 8086710);
//        mgLog.d("xxxy1 "+countNeighbours(xxxGN) );
//        short xNeighbour = nodes.getNeighbour(xxxGN);
//        while (xNeighbour != 0){
//            short gnn = neighbours.getNeighbourPoint(xNeighbour);
//            mgLog.d( nodes.getLatitude(gnn)+","+nodes.getLongitude(gnn));
//            xNeighbour = neighbours.getNextNeighbour(xNeighbour);
//        }

        BNode aiBNode = tmBNodes.firstKey();
        int ai = 0;
        while (aiBNode != null){
            mgLog.d(String.format(Locale.ENGLISH,"yyyy %4d lat=%.6f,lon=%.6f", ai,aiBNode.getLat()/1000000.0,aiBNode.getLon()/1000000.0));
            aiBNode = tmBNodes.higherKey(aiBNode);
            ai++;
        }

        int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER) );
        int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
        //all relevant ways are in the map ... try to correct data ...
        int numPoints = nodes.pointsUsed;
        BNode iBNode = tmBNodes.firstKey();
        short iiIdx = 0;
        while (iBNode != null){
            short iIdx = iBNode.nodeIdx;
            int iLat = nodes.getLatitude(iIdx);
            int iLon = nodes.getLongitude(iIdx);
            if (!nodes.isFlag(iIdx, FLAG_INVALID)){ // valid point
                int iNeighbours = countNeighbours(iIdx);

                BNode nBNode = tmBNodes.higherKey(iBNode);
                short nnIdx = (short)(iiIdx+1);
                while (nBNode != null){
                    short nIdx = nBNode.nodeIdx;

                    int nLat = nodes.getLatitude(nIdx);
                    int nLon = nodes.getLongitude(nIdx);
                    if (!nodes.isFlag(nIdx, FLAG_INVALID)){ // valid point

                        if ((Math.abs(nLat - iLat) < latThreshold) && (Math.abs(nLon - iLon) < lonThreshold) &&
                                PointModelUtil.distance(LaLo.md2d(iLat),LaLo.md2d(iLon), LaLo.md2d(nLat),LaLo.md2d(nLon)) <= CONNECT_THRESHOLD_METER){ // connect candidate
                            if (getNeighbour(iIdx,nIdx) == 0){ // // is not yet neighbour
                                int nNeighbours = countNeighbours(nIdx);
                                assert ((iNeighbours != 0) && (nNeighbours != 0)) ; // don't connect, if a node has no neighbours (might occur due to former reduceGraph action)

//                                mgLog.d("XXXX-compare add iiIdx="+iiIdx+" nnIdx="+nnIdx+"     iIdx="+iIdx+" nIdx="+nIdx);

                                if ((iNeighbours+nNeighbours != 3) || nodes.isBorderPoint(nIdx) || nodes.isBorderPoint(iIdx)){
                                    mgLog.d("XXXX-fix add iIdx="+iIdx+" nIdx="+nIdx);
                                    addSegment((short)-1,iIdx, nIdx);
                                } else { // iNeighbours:nNeighbours is either 2:1 or 1:2
                                    short iDrop = (iNeighbours==1)?iIdx:nIdx;
                                    short iDropReplace = (iNeighbours==2)?iIdx:nIdx;
                                    reduceGraph(iDrop, iDropReplace); // drop iNode; move neighbour form iNode to nNode
                                }

                            } // if (getNeighbour(iIdx,nIdx) == 0){ // // is not yet neighbour
                        } // connect candidate

                    } // if ((nLat != PointModel.NO_LAT_LONG_MD) && (nLon != PointModel.NO_LAT_LONG_MD)) { // valid point
                    nBNode = tmBNodes.higherKey(nBNode);
                    nnIdx++;
                } // while (nBNode != null){
            } // if ((iLat != PointModel.NO_LAT_LONG_MD) && (iLon != PointModel.NO_LAT_LONG_MD)){ // valid point
            iBNode = tmBNodes.higherKey(iBNode);
            iiIdx++;
        }

//        mgLog.d("xxxy2 "+countNeighbours(xxxGN) );
//        xNeighbour = nodes.getNeighbour(xxxGN);
//        while (xNeighbour != 0){
//            short gnn = neighbours.getNeighbourPoint(xNeighbour);
//            mgLog.d( nodes.getLatitude(gnn)+","+nodes.getLongitude(gnn));
//            xNeighbour = neighbours.getNextNeighbour(xNeighbour);
//        }


        byte[] baPoints = new byte[nodes.pointsUsed* POINT_SIZE];
        System.arraycopy(baPointsInit, 0, baPoints, 0, nodes.pointsUsed* POINT_SIZE);
        nodes.init( ByteBuffer.wrap(baPoints) );
        int numBorderNodes = nodes.countBorderNodes();
        byte[] baNeighbours = new byte[neighbours.neighboursUsed* NEIGHBOUR_SIZE + numBorderNodes*NEIGHBOUR_SIZE * 4]; // numBorderNodes*NEIGHBOUR_SIZE*4 is preserved space for tile interconnect - per interconnect we need two neighbours
        System.arraycopy(baNeighboursInit, 0, baNeighbours, 0, neighbours.neighboursUsed* NEIGHBOUR_SIZE);
        neighbours.init(ByteBuffer.wrap(baNeighbours));
        wayAttributes = new WayAttributs[wayAttributesUsed];
        System.arraycopy(wayAttributesInit, 0, wayAttributes, 0, wayAttributesUsed);

        freeNodes.addAll(tmBNodes.keySet());
        tmBNodes.clear();
        assert (freeNodes.size() == MAX_POINTS);
        init = false;
    }

    void addLatLongs(short wayAttributesIdx, LatLong[] latLongs){
        assert(init);
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
                addSegment(wayAttributesIdx, LaLo.d2md(lat1), LaLo.d2md(lon1) ,LaLo.d2md(lat2), LaLo.d2md(lon2));
            }
        }
    }

    void addSegment(short wayAttributesIdx, int lat1, int lon1, int lat2, int lon2){
        assert(init);
        short pt1Idx = getAddPoint(lat1, lon1);
        short pt2Idx = getAddPoint(lat2, lon2);
        addSegment(wayAttributesIdx, pt1Idx, pt2Idx);
    }


    // get point index for given lat+lon; if such point is not yet registered, add this point and return its index
    private short getAddPoint(int lat, int lon){
        assert(init);

        short ptIdx = nodes.createNode(lat, lon);
        BNode bNode = freeNodes.remove(0);
        bNode.setBGraphTile(this);
        bNode.setNodeIdx(ptIdx);

        BNode existingBNode = tmBNodes.putIfAbsent(bNode, bNode);
        if (existingBNode == null){

            byte borderNode = 0;
            if (lon == minLon) borderNode |= BORDER_NODE_WEST;
            if (lon == maxLon) borderNode |= BORDER_NODE_EAST;
            if (lat == minLat)  borderNode |= BORDER_NODE_SOUTH;
            if (lat == maxLat)  borderNode |= BORDER_NODE_NORTH;

            hgtTemp.setLa(lat);
            hgtTemp.setLo(lon);
            elevationProvider.setElevation(hgtTemp);

            ptIdx = nodes.createNode(lat, lon, hgtTemp.getEle(), borderNode);
            short ptNIdx = neighbours.createNeighbour((short)-1, ptIdx, 0, REVERSE_NO);
            nodes.setNeighbour(ptIdx, ptNIdx);

            nodes.setEle(ptIdx, hgtTemp.getEle());
            final short ppp = ptIdx;
            mgLog.d(()->String.format(Locale.ENGLISH, "addNode idx=%d lat=%.6f lon=%.6f ",ppp, LaLo.md2d(lat),LaLo.md2d(lon) ));
        } else {
            freeNodes.add(bNode);
            ptIdx = existingBNode.nodeIdx;
        }
        return ptIdx;
    }

    // reduce Graph by dropping node identified by pointIdxDrop, all Neighbours form pointIdxDrop will get pointIdxReplace as a Neighbour
    private void reduceGraph(short pointIdxDrop, short pointIdxReplace){
        assert(init);
        short nIdx = nodes.getNeighbour(pointIdxDrop);
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            short waIdx = neighbours.getWayAttributes(nIdx);

            mgLog.d(()->"XXXX reduceGraph pointIdxDrop="+pointIdxDrop+"("+nodes.getLatitude(pointIdxDrop)+","+nodes.getLongitude(pointIdxDrop)+")"
                    +" pointIdxReplace="+pointIdxReplace+"("+nodes.getLatitude(pointIdxReplace)+","+nodes.getLongitude(pointIdxReplace)+")"
                    +" npIdx="+npIdx+"("+nodes.getLatitude(npIdx)+","+nodes.getLongitude(npIdx)+")");
            removeNeighbourTo(npIdx, pointIdxDrop);
            addSegment(waIdx, pointIdxReplace, npIdx);
        }
        nodes.setFlag(pointIdxDrop, FLAG_INVALID, true);
    }

    public void removeNeighbourTo(short pointIdx, short pointIdxNeighbour) { // assume both points are in same tile
        assert (init);
        short nIdx = nodes.getNeighbour(pointIdx);

        short lastNIdx = nIdx;
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            short nnIdx = neighbours.getNextNeighbour(nIdx);
            if (npIdx == pointIdxNeighbour){
                neighbours.setNextNeighbour(lastNIdx, nnIdx);
                break;
            }
            lastNIdx = nIdx;
        }
    }







    // ++++++++++++++++++++++++++++++++++++++++++ Methods for smoothing ++++++++++++++++++++++++++++++++++++++++++++++

    void smoothGraph(){
        ArrayList<Short> smoothNeighbourList = new ArrayList<>();
        for (short pointIdx = 0; pointIdx < nodes.pointsUsed; pointIdx++){
            boolean fix = true;
            if (countNeighbours(pointIdx) == 2){
                short selfNeighbour = nodes.getNeighbour(pointIdx);
                short firstNeighbour = neighbours.getNextNeighbour(selfNeighbour);
                short secondNeighbour = neighbours.getNextNeighbour(firstNeighbour);
                if (neighbours.getWayAttributes(firstNeighbour) == neighbours.getWayAttributes(secondNeighbour)){
                    fix = false;
                }
            }
            nodes.setFlags(pointIdx, FLAG_FIX, fix, FLAG_VISITED, false, FLAG_HEIGHT_RELEVANT, fix);
        }

        for (short aPointIdx = 0; aPointIdx < nodes.pointsUsed; aPointIdx++) {
            if (nodes.isFlag(aPointIdx, FLAG_FIX)){
                short minHeightPoint;
                short maxHeightPoint;
                short aNeighbourIdx = nodes.getNeighbour(aPointIdx);
                while ((aNeighbourIdx = neighbours.getNextNeighbour(aNeighbourIdx)) != 0){
                    short neighbourIdx = aNeighbourIdx;
                    short pointIndexNeighbour = neighbours.getNeighbourPoint(aNeighbourIdx);
                    if (nodes.isFlag(pointIndexNeighbour, FLAG_VISITED)) continue; // this path is already handled
                    int neighbourNodeIdx; // index to SmoothingList
                    // reset smoothNodeList
                    smoothNeighbourList.clear();
                    smoothNeighbourList.add(nodes.getNeighbour(aPointIdx)); // neighbour with getNeighbourNode = node
                    float minHeight = nodes.getEle(aPointIdx);
                    float maxHeight = minHeight;
                    minHeightPoint = aPointIdx;
                    maxHeightPoint = aPointIdx;
                    int minHeightPointIdx = 0; // index to SmoothingList
                    int maxHeightPointIdx = 0; // index to SmoothingList
                    short lastHeightRelevantPoint = aPointIdx;
                    int lastHeightRelevantPointIdx = 0;
                    int signumLastHeightInterval = 0;
                    int signumHeightInterval = 0;

                    while (true){
                        nodes.setFlag(pointIndexNeighbour, FLAG_VISITED, true);
                        neighbourNodeIdx = smoothNeighbourList.size();
                        smoothNeighbourList.add(neighbourIdx);
                        float neighbourNodeEle = nodes.getEle(pointIndexNeighbour);
                        if (neighbourNodeEle < minHeight){
                            minHeightPoint = pointIndexNeighbour;
                            minHeight = neighbourNodeEle;
                            minHeightPointIdx = neighbourNodeIdx;
                        }
                        if (neighbourNodeEle > maxHeight){
                            maxHeightPoint = pointIndexNeighbour;
                            maxHeight = neighbourNodeEle;
                            maxHeightPointIdx = neighbourNodeIdx;
                        }

                        if ( (maxHeight - minHeight >= TrackLogStatistic.ELE_THRESHOLD_ELSE) && (distance(smoothNeighbourList, minHeightPointIdx, maxHeightPointIdx) > PointModelUtil.getCloseThreshold()/2d)){
                            nodes.setFlag(pointIndexNeighbour, FLAG_HEIGHT_RELEVANT, true);
                            if ( maxHeight == neighbourNodeEle ){
                                signumHeightInterval = 1;
                                if (minHeightPoint != lastHeightRelevantPoint){
                                    if (!nodes.isFlag(lastHeightRelevantPoint, FLAG_FIX)){
                                        nodes.setFlag(minHeightPoint, FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, minHeightPointIdx) <  PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( nodes.getEle(minHeightPoint) - nodes.getEle(lastHeightRelevantPoint) ) ) nodes.setFlag(lastHeightRelevantPoint, FLAG_HEIGHT_RELEVANT, false); // reset height relevance
                                        }
                                    }

                                }
                            }
                            if ( minHeight == neighbourNodeEle ){
                                signumHeightInterval = -1;
                                if (maxHeightPoint != lastHeightRelevantPoint){
                                    if (!nodes.isFlag(lastHeightRelevantPoint,FLAG_FIX)) {
                                        nodes.setFlag(maxHeightPoint, FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, maxHeightPointIdx) < PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( nodes.getEle(maxHeightPoint) - nodes.getEle(lastHeightRelevantPoint) ) ) nodes.setFlag(lastHeightRelevantPoint, FLAG_HEIGHT_RELEVANT, false); // reset height relevance
                                        }
                                    }
                                }
                            }
                            lastHeightRelevantPoint = pointIndexNeighbour;
                            lastHeightRelevantPointIdx = neighbourNodeIdx;
                            signumLastHeightInterval = signumHeightInterval;
                            minHeight = neighbourNodeEle;
                            maxHeight = neighbourNodeEle;
                            minHeightPoint = pointIndexNeighbour;
                            maxHeightPoint = pointIndexNeighbour;

                        }
                        if (nodes.isFlag(pointIndexNeighbour, FLAG_FIX)) break; // main exit from loop!

                        neighbourIdx = oppositeNeighbour(pointIndexNeighbour, neighbours.getReverse(neighbourIdx));
                        pointIndexNeighbour = neighbours.getNeighbourPoint(neighbourIdx);
                    } // while true
                    if (!nodes.isFlag(lastHeightRelevantPoint, FLAG_FIX) &&
                            (distance (smoothNeighbourList,  lastHeightRelevantPointIdx, neighbourNodeIdx) < PointModelUtil.getCloseThreshold()/2d)){
                        nodes.setFlag(lastHeightRelevantPoint, FLAG_HEIGHT_RELEVANT, false); // reset last heightRelevantPoint in this segment - otherwise the remaining
                    }

                    mgLog.v(()->neighbours.getNeighbourPoint(smoothNeighbourList.get(0))+" --- "+neighbours.getNeighbourPoint(smoothNeighbourList.get(smoothNeighbourList.size()-1))+" ("+smoothNeighbourList.size()+")");

                    if (smoothNeighbourList.size() <= 2) continue;

                    int startIdx = 0;
                    while (startIdx < smoothNeighbourList.size() -1){
                        int endIdx = startIdx;
                        while ((startIdx == endIdx) || !nodes.isFlag( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)), FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                        }
                        float startHeight = nodes.getEle( neighbours.getNeighbourPoint( smoothNeighbourList.get(startIdx)) );
                        float endHeight = nodes.getEle( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)) );
                        double totalDistance = distance(smoothNeighbourList, startIdx, endIdx);

                        endIdx = startIdx;
                        double distance = 0;
                        while ((startIdx == endIdx) || !nodes.isFlag( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)), FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                            short endNode = neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx));
                            if (!nodes.isFlag( endNode, FLAG_HEIGHT_RELEVANT)){
                                distance += neighbours.getDistance( smoothNeighbourList.get(endIdx));
                                float height = (float)PointModelUtil.interpolate (0, totalDistance, startHeight, endHeight, distance);
                                nodes.setEle(endNode, height);
                            }
                        }

                        startIdx = endIdx;
                    }
                } // iterate over all neighbours
            } // if (isFlag(aPointIdx, FLAG_FIX))
        } // iterate over all points
    }

    private double distance(ArrayList<Short> smoothNeighbourList, int idx1, int idx2){
        if (idx1 > idx2){
            return distance(smoothNeighbourList, idx2, idx1);
        }
        double distance = 0;
        for (int idx=idx1+1; idx <= idx2; idx++){
            distance += neighbours.getDistance( smoothNeighbourList.get(idx) );
        }
        return distance;
    }







    // ++++++++++++++++++++++++++++++++++++++++++ Methods used all times +++++++++++++++++++++++++++++++++++++++++++++


    public short getNeighbour(short nodeIdx, short pointIdxNeighbour){ // assume both points are in same tile
        short nIdx = nodes.getNeighbour(nodeIdx);
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            if (npIdx == pointIdxNeighbour) return nIdx;
        }
        return 0;
    }


    public void addNeighbour(short pointIdx, short neighbourIdx){
        short nIdx = nodes.getNeighbour(pointIdx);
        short nnIdx;
        while ((nnIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            nIdx = nnIdx;
        }
        neighbours.setNextNeighbour(nIdx, neighbourIdx);
        mgLog.d(String.format(Locale.ENGLISH, "addNeighbour point=%d neighbour=%d ",pointIdx,neighbourIdx ));
    }


    public int countNeighbours(short pointIdx){
        short nIdx = nodes.getNeighbour(pointIdx);
        int cnt = 0;
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            cnt++;
        }
        return cnt;
    }

    void addSegment(short wayAttributesIdx, short pt1Idx, short pt2Idx) {
        mgLog.d(String.format(Locale.ENGLISH, "addSegment widx=%d pt1Idx=%d pt2Idx=%d",wayAttributesIdx,pt1Idx,pt2Idx ));
        double dLat1 = LaLo.md2d(nodes.getLatitude(pt1Idx));
        double dLon1 = LaLo.md2d(nodes.getLongitude(pt1Idx));
        double dLat2 = LaLo.md2d(nodes.getLatitude(pt2Idx));
        double dLon2 = LaLo.md2d(nodes.getLongitude(pt2Idx));
        double distance = PointModelUtil.distance(dLat1, dLon1, dLat2, dLon2);
        short n12Idx = neighbours.createNeighbour(wayAttributesIdx, pt2Idx, (float) distance, REVERSE_NEXT);
        short n21Idx = neighbours.createNeighbour(wayAttributesIdx, pt1Idx, (float) distance, REVERSE_PREV);
        addNeighbour(pt1Idx, n12Idx);
        addNeighbour(pt2Idx, n21Idx);
    }



    public short oppositeNeighbour(short pointIdx, short givenNeighbour){
        short selfNeighbour = nodes.getNeighbour(pointIdx);
        short firstNeighbour = neighbours.getNextNeighbour(selfNeighbour);
        if (firstNeighbour == 0) return 0; // found no neighbour
        short secondNeighbour = neighbours.getNextNeighbour(firstNeighbour);
        if (secondNeighbour == 0) return 0; // found just one neighbour
        short thirdNeighbour = neighbours.getNextNeighbour(secondNeighbour);
        if (thirdNeighbour != 0) return 0; // found third neighbour
        if (firstNeighbour == givenNeighbour){
            return secondNeighbour;
        }
        if (secondNeighbour == givenNeighbour){
            return firstNeighbour;
        }
        return 0; // should not happen (given neighbourNode is not neighbour to node
    }


    @Override
    public ArrayList<MultiPointModel> getRawWays() {
        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                MultiPointModelImpl mpm = new MultiPointModelImpl();
                for (LatLong latLong : way.latLongs[0] ){
                    mpm.addPoint(new PointModelImpl(latLong));

                }
                rawWays.add(mpm);
            }
        }
        return rawWays;
    }

    @Override
    public Boolean sameGraph(PointModel node1, PointModel node2) {
        return Graph.super.sameGraph(node1, node2);
    }

    @Override
    public ArrayList<? extends PointModel> getNodes() {
        ArrayList<BNode> bNodes = new ArrayList<>();
        for (short nIdx=0; nIdx<nodes.pointsUsed; nIdx++){
            if (!nodes.isFlag(nIdx, FLAG_INVALID)){
                bNodes.add( new BNode(this, nIdx) );
            }
        }
        return bNodes;
    }

    @Override
    public PointNeighbour getNeighbour(PointModel node, PointModel neighbourNode) {
        if ((node instanceof BNode bNode) && (neighbourNode instanceof  BNode bNeighbourNode)){
            return getNeighbour(bNode, bNeighbourNode);
        }
        return null;
    }
    public BNeighbour getNeighbour(BNode node, BNode neighbourNode){
        short neighbourIdx = nodes.getNeighbour(node.nodeIdx);
        if (node.nodeIdx != neighbourNode.nodeIdx){
            neighbourIdx = getNeighbour(node.nodeIdx, neighbourNode.nodeIdx);
        }
        return (neighbourIdx==0)?null:new BNeighbour(this, neighbourIdx);
    }


    @Override
    public PointNeighbour getNextNeighbour(PointModel node, PointNeighbour neighbour) {
        if (neighbour instanceof  BNeighbour bNeighbour){
            if (neighbours.getTileSelector(bNeighbour.neighbourIdx) == 0){
                return new BNeighbour(this, neighbours.getNextNeighbour(bNeighbour.neighbourIdx));
            }
        }
        return null;
    }

    @Override
    public ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2, int closeThreshold, boolean limitToTile) {
        assert (limitToTile): "Unsupported operation";
        ArrayList<PointModel> segmentNodes = new ArrayList<>();
        segmentNodes.add(node1);
        segmentNodes.add(node2);
        if ((node1 instanceof BNode bNode1) && (node2 instanceof  BNode bNode2)){
            short nodeIdxA = bNode1.nodeIdx;
            short nodeIdxB = bNode2.nodeIdx;
            short neighbourIdx = getNeighbour(nodeIdxA, nodeIdxB);
            float distance = 0;
            short oppositeNeighbour;
            while ( (oppositeNeighbour = oppositeNeighbour( neighbours.getNeighbourPoint(neighbourIdx), neighbourIdx )) != 0){
                neighbourIdx = neighbours.getReverse(oppositeNeighbour);
                segmentNodes.add(new BNode(this, neighbours.getNeighbourPoint(neighbourIdx)));
                distance += neighbours.getDistance(neighbourIdx);
                if (distance >= closeThreshold) break;
            }
            nodeIdxA = bNode2.nodeIdx;
            nodeIdxB = bNode1.nodeIdx;
            neighbourIdx = getNeighbour(nodeIdxA, nodeIdxB);
            distance = 0;
            while ( (oppositeNeighbour = oppositeNeighbour( neighbours.getNeighbourPoint(neighbourIdx), neighbourIdx )) != 0){
                neighbourIdx = neighbours.getReverse(oppositeNeighbour);
                segmentNodes.add(new BNode(this, neighbours.getNeighbourPoint(neighbourIdx)));
                distance += neighbours.getDistance(neighbourIdx);
                if (distance >= closeThreshold) break;
            }
        }
        return segmentNodes;
    }

    @Override
    public String getRefDetails(PointModel node) {
        return "";
    }

    @Override
    public float getCost(PointNeighbour neighbour) {
        float cost = 0;
        if (neighbour instanceof  BNeighbour bNeighbour){
                cost = neighbours.getCost(bNeighbour.neighbourIdx);
        }
        return cost;
    }

    @Override
    public BBox getBBox() {
        return bBox;
    }


}
