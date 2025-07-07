package mg.mgmap.generic.graph.test.bgraph;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.WayAttributs;

import mg.mgmap.generic.graph.implbb.BNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BTile {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m
    private final static int MD_TILE_SIZE = 10986;

    final static int MAX_NODES = 8192; // per tile

    final static int MAX_NEIGHBOURS = 8192*8; // per tile

    final static int MAX_WAY_ATTRIBUTES = 8192;
    final static WayAttributs[] wayAttributesInit = new WayAttributs[MAX_WAY_ATTRIBUTES];

    final static byte[] baNodesInit = new byte[BNodes.NODE_SIZE * MAX_NODES];
    final static ByteBuffer bbNodesInit = ByteBuffer.wrap(baNodesInit);
    final static byte[] baNeighboursInit = new byte[BNeighbours.NEIGHBOUR_SIZE * MAX_NEIGHBOURS];
    final static ByteBuffer bbNeighboursInit = ByteBuffer.wrap(baNeighboursInit);
    final static ArrayList<Short> sortedNodes = new ArrayList<>();

    static ArrayList<mg.mgmap.generic.graph.implbb.BNode> freeNodes = new ArrayList<>();
    static {
        for (int i = 0; i< MAX_NODES; i++){
            freeNodes.add(new mg.mgmap.generic.graph.implbb.BNode());
        }
    }

    static final byte BORDER_WEST = 0x08;
    static final byte BORDER_NORTH = 0x04;
    static final byte BORDER_EAST = 0x02;
    static final byte BORDER_SOUTH = 0x01;
    static final byte BORDER_NO = 0x00;

    public static int deltaX(byte border){
        return switch (border) {
            case BORDER_WEST -> -1;
            case BORDER_EAST -> 1;
            default -> 0;
        };
    }
    public static int deltaY(byte border){
        return switch (border) {
            case BORDER_NORTH -> -1;
            case BORDER_SOUTH -> 1;
            default -> 0;
        };
    }

    // ************* member definitions

    ElevationProvider elevationProvider;
    final Tile tile;

    BNodes nodes = new BNodes();
    BNeighbours neighbours = new BNeighbours();
    WayAttributs[] wayAttributes;
    short wayAttributesUsed = 0;

    final BBox bBox;
    int minLat;
    int maxLat;
    int minLon;
    int maxLon;

    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    private final WriteablePointModel hgtTemp = new WriteablePointModelImpl();
    boolean init = true; // after initial setup this flag is set to false - access to static arrays is only allowed while init flag is set

    public BTile(ElevationProvider elevationProvider, Tile tile){
        this.elevationProvider = elevationProvider;
        this.tile = tile;
        this.bBox = BBox.fromBoundingBox(tile.getBoundingBox());
        minLat = LaLo.d2md(bBox.minLatitude);
        maxLat = LaLo.d2md(bBox.maxLatitude);
        minLon = LaLo.d2md(bBox.minLongitude);
        maxLon = LaLo.d2md(bBox.maxLongitude);
    }

    public void init( List<Way> ways){
        nodes.init(bbNodesInit);
        nodes.nodesUsed = 0;
        neighbours.init(bbNeighboursInit);
        neighbours.neighboursUsed = 1; // idx 0 is used as "null"
        this.wayAttributes = wayAttributesInit;
        wayAttributesUsed = 0;
        sortedNodes.clear();

        for (Way way : ways) {
            short waIdx = wayAttributesUsed++;
            wayAttributes[waIdx] = new WayAttributs(way);
            addLatLongs( waIdx, way.latLongs[0]);

        }

        fixIt();
        smoothGraph();

        byte[] baPoints = new byte[nodes.nodesUsed * BNodes.NODE_SIZE];
        System.arraycopy(baNodesInit, 0, baPoints, 0, nodes.nodesUsed * BNodes.NODE_SIZE);
        nodes.init( ByteBuffer.wrap(baPoints) );
        // nodesUsed is already set from the init phase
        int numBorderNodes = nodes.countBorderNodes();
        byte[] baNeighbours = new byte[neighbours.neighboursUsed* BNeighbours.NEIGHBOUR_SIZE + numBorderNodes*BNeighbours.NEIGHBOUR_SIZE * 4]; // numBorderNodes*NEIGHBOUR_SIZE*4 is preserved space for tile interconnect - per interconnect we need two neighbours
        System.arraycopy(baNeighboursInit, 0, baNeighbours, 0, neighbours.neighboursUsed* BNeighbours.NEIGHBOUR_SIZE);
        neighbours.init(ByteBuffer.wrap(baNeighbours));
        wayAttributes = new WayAttributs[wayAttributesUsed];
        System.arraycopy(wayAttributesInit, 0, wayAttributes, 0, wayAttributesUsed);

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
                addSegment(wayAttributesIdx, lat1, lon1 ,lat2, lon2);
            }
        }
    }

    void addSegment(short wayAttributesIdx, double lat1, double lon1, double lat2, double lon2){
        assert(init);
        short pt1Idx = getAddNode(lat1, lon1);
        short pt2Idx = getAddNode(lat2, lon2);
        addSegment(wayAttributesIdx, pt1Idx, pt2Idx);
    }

    public short getNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, false);
    }
    short getAddNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, true);
    }
    short getAddNode(double latitude, double longitude, boolean allowAdd){
        return getAddNode(PointModelUtil.roundMD(latitude), PointModelUtil.roundMD(longitude), (short)-1, nodes.nodesUsed, allowAdd);
    }


    /**
     * !!! GNode object in ArrayList nodes are stored sorted to retrieve already existing points fast !!!
     * This is only done in GGraphTile during setup of the graph.
     * @param latitude latitude of the point to be stored
     * @param longitude longitude of the point to be stored
     * @param low nodes.get(low) is strict less (or low is -1)
     * @param high nodes.get(high) is strict greater than (or high is nodes.size() )
     * @param allowAdd if set, then add a new node, if there is none with these lat/lon values (false is used for pure search)
     * @return GNode with latitude an longitude, either already existing point or newly created one
     */
    private short getAddNode(double latitude, double longitude, short low, short high, boolean allowAdd){
        if (high - low == 1){ // nothing more to compare, insert a new GNode at high index
            if (allowAdd){
                // use hgtTemp as parameter by reference to ElevationProvider, that return the hgtEle and the hgtEleAcc values via this reference - Since GNode is no WritablePointModel, it cannot be used directly for this.
                hgtTemp.setLat(latitude);
                hgtTemp.setLon(longitude);
                elevationProvider.setElevation(hgtTemp);
                byte borderNode = BORDER_NO;
                if (longitude == bBox.minLongitude) borderNode |= BORDER_WEST;
                if (longitude == bBox.maxLongitude) borderNode |= BORDER_EAST;
                if (latitude == bBox.minLatitude)  borderNode |= BORDER_SOUTH;
                if (latitude == bBox.maxLatitude)  borderNode |= BORDER_NORTH;
                short node = nodes.createNode(LaLo.d2md(latitude), LaLo.d2md(longitude), hgtTemp.getEle(), borderNode);
                short neighbour = neighbours.createNeighbour((short)-1, node, 0, BORDER_NO, BNeighbours.PRIMARY_NO);
                nodes.setNeighbour(node, neighbour);

                sortedNodes.add(high, node);
                return node;
            } else {
                return -1;
            }
        } else {
            short mid = (short)((high + low) /2);
            short midNode = sortedNodes.get(mid);
            int cmp = PointModelUtil.compareTo(latitude, longitude, LaLo.md2d(nodes.getLatitude(midNode)), LaLo.md2d(nodes.getLongitude(midNode)) );
            if (cmp == 0) return mid;
            if (cmp < 0){
                return getAddNode(latitude, longitude, low, mid, allowAdd);
            } else {
                return getAddNode(latitude, longitude, mid, high, allowAdd);
            }
        }
    }

    void addSegment(short wayAttributesIdx, short pt1Idx, short pt2Idx) {
        addSegment(wayAttributesIdx, pt1Idx, this, pt2Idx, BORDER_NO);
    }

    void addSegment(short wayAttributesIdx, short pt1Idx, BTile bTile2, short pt2Idx, byte bGraphTile2Selector) {
//        mgLog.d(String.format(Locale.ENGLISH, "addSegment wIdx=%d pt1Idx=%d tileX=%d tileY=%d pt2Idx=%d",wayAttributesIdx,pt1Idx,bTile2.tile.tileX,bTile2.tile.tileY, pt2Idx ));
        double dLat1 = LaLo.md2d(nodes.getLatitude(pt1Idx));
        double dLon1 = LaLo.md2d(nodes.getLongitude(pt1Idx));
        double dLat2 = LaLo.md2d(bTile2.nodes.getLatitude(pt2Idx));
        double dLon2 = LaLo.md2d(bTile2.nodes.getLongitude(pt2Idx));
        double distance = PointModelUtil.distance(dLat1, dLon1, dLat2, dLon2);
        short n12Idx = neighbours.createNeighbour(wayAttributesIdx, pt2Idx, (float) distance, bGraphTile2Selector, BNeighbours.PRIMARY_YES);
        short n21Idx = neighbours.createNeighbour(wayAttributesIdx, pt1Idx, (float) distance, bGraphTile2Selector, (this==bTile2)?BNeighbours.PRIMARY_NO:BNeighbours.PRIMARY_YES); // for tile connectors use always PRIMARY_YES
        addNeighbour(pt1Idx, n12Idx);
        if (this == bTile2){
            addNeighbour(pt2Idx, n21Idx); // if segment is to another tile, then you cannot add the reverse neighbour to the node here - in this case the add segment is called 2nd time for the other tle
        }
    }

    public void addNeighbour(short pointIdx, short neighbourIdx){
        short nIdx = nodes.getNeighbour(pointIdx);
        short nnIdx;
        while ((nnIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            nIdx = nnIdx;
        }
        neighbours.setNextNeighbour(nIdx, neighbourIdx);
//        mgLog.d(String.format(Locale.ENGLISH, "addNeighbour point=%d neighbour=%d ",pointIdx,neighbourIdx ));
    }





    public void fixIt() {
        int latThreshold = LaLo.d2md(PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER));
        int lonThreshold = LaLo.d2md(PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()));
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
        //all relevant ways are in the map ... try to correct data ...
        int numPoints = nodes.nodesUsed;
        for (short iIdx = 0; iIdx < sortedNodes.size(); iIdx++) {
            int iLat = nodes.getLatitude(iIdx);
            int iLon = nodes.getLongitude(iIdx);
            if (!nodes.isFlag(iIdx, BNodes.FLAG_INVALID)) { // valid point
                int iNeighbours = countNeighbours(iIdx);

                for (short nIdx = (short)(iIdx + 1); nIdx < sortedNodes.size(); nIdx++) {

                    int nLat = nodes.getLatitude(nIdx);
                    int nLon = nodes.getLongitude(nIdx);
                    if (!nodes.isFlag(nIdx, BNodes.FLAG_INVALID)) { // valid point

                        if ((Math.abs(nLat - iLat) < latThreshold) && (Math.abs(nLon - iLon) < lonThreshold) &&
                                PointModelUtil.distance(LaLo.md2d(iLat), LaLo.md2d(iLon), LaLo.md2d(nLat), LaLo.md2d(nLon)) <= CONNECT_THRESHOLD_METER) { // connect candidate
                            if (getNeighbour(iIdx, nIdx) == 0) { // // is not yet neighbour
                                int nNeighbours = countNeighbours(nIdx);
                                assert ((iNeighbours != 0) && (nNeighbours != 0)); // don't connect, if a node has no neighbours (might occur due to former reduceGraph action)

                                if ((iNeighbours + nNeighbours != 3) || nodes.isBorderPoint(nIdx) || nodes.isBorderPoint(iIdx)) {
//                                    mgLog.d("XXXX-fix add iIdx=" + iIdx + " nIdx=" + nIdx);
                                    addSegment((short) -1, iIdx, nIdx);
                                } else { // iNeighbours:nNeighbours is either 2:1 or 1:2
                                    short iDrop = (iNeighbours == 1) ? iIdx : nIdx;
                                    short iDropReplace = (iNeighbours == 2) ? iIdx : nIdx;
                                    reduceGraph(iDrop, iDropReplace); // drop iNode; move neighbour form iNode to nNode
                                }

                            } // if (getNeighbour(iIdx,nIdx) == 0){ // // is not yet neighbour
                        } // connect candidate

                    } // if ((nLat != PointModel.NO_LAT_LONG_MD) && (nLon != PointModel.NO_LAT_LONG_MD)) { // valid point
                } // while (nBNode != null){
            } // if ((iLat != PointModel.NO_LAT_LONG_MD) && (iLon != PointModel.NO_LAT_LONG_MD)){ // valid point


        }
    }

    public int countNeighbours(short pointIdx){
        short nIdx = nodes.getNeighbour(pointIdx);
        int cnt = 0;
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            cnt++;
        }
        return cnt;
    }

    public short getNeighbour(short nodeIdx, short pointIdxNeighbour){ // assume both points are in same tile
        short nIdx = nodes.getNeighbour(nodeIdx);
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            if (npIdx == pointIdxNeighbour) return nIdx;
        }
        return 0;
    }



    // reduce Graph by dropping node identified by pointIdxDrop, all Neighbours form pointIdxDrop will get pointIdxReplace as a Neighbour
    private void reduceGraph(short pointIdxDrop, short pointIdxReplace){
        assert(init);
        short nIdx = nodes.getNeighbour(pointIdxDrop);
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            short waIdx = neighbours.getWayAttributes(nIdx);

//            mgLog.d(()->"XXXX reduceGraph pointIdxDrop="+pointIdxDrop+"("+nodes.getLatitude(pointIdxDrop)+","+nodes.getLongitude(pointIdxDrop)+")"
//                    +" pointIdxReplace="+pointIdxReplace+"("+nodes.getLatitude(pointIdxReplace)+","+nodes.getLongitude(pointIdxReplace)+")"
//                    +" npIdx="+npIdx+"("+nodes.getLatitude(npIdx)+","+nodes.getLongitude(npIdx)+")");
            removeNeighbourTo(npIdx, pointIdxDrop);
            addSegment(waIdx, pointIdxReplace, npIdx);
        }
        nodes.setFlag(pointIdxDrop, BNodes.FLAG_INVALID, true);
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

    // remove neighbours to tile with given selector
    public void removeNeighbourToTile(short nodeIdx, byte neighbourTileSelector) { // assume both points are in same tile
        short nIdx = nodes.getNeighbour(nodeIdx);

        short lastNIdx = nIdx;
        while ((nIdx = neighbours.getNextNeighbour(nIdx)) != 0){
            short npIdx = neighbours.getNeighbourPoint(nIdx);
            short nnIdx = neighbours.getNextNeighbour(nIdx);
            byte tileSelector = neighbours.getTileSelector(nIdx);
            if (tileSelector == neighbourTileSelector){
                neighbours.setNextNeighbour(lastNIdx, nnIdx);
                break;
            }
            lastNIdx = nIdx;
        }
    }



    void smoothGraph(){
        ArrayList<Short> smoothNeighbourList = new ArrayList<>();
        for (short pointIdx = 0; pointIdx < nodes.nodesUsed; pointIdx++){
            boolean fix = true;
            if (countNeighbours(pointIdx) == 2){
                short selfNeighbour = nodes.getNeighbour(pointIdx);
                short firstNeighbour = neighbours.getNextNeighbour(selfNeighbour);
                short secondNeighbour = neighbours.getNextNeighbour(firstNeighbour);
                if (neighbours.getWayAttributes(firstNeighbour) == neighbours.getWayAttributes(secondNeighbour)){
                    fix = false;
                }
            }
            nodes.setFlags(pointIdx, BNodes.FLAG_FIX, fix, BNodes.FLAG_VISITED, false, BNodes.FLAG_HEIGHT_RELEVANT, fix);
        }

        for (short aPointIdx = 0; aPointIdx < nodes.nodesUsed; aPointIdx++) {
            if (nodes.isFlag(aPointIdx, BNodes.FLAG_FIX)){
                short minHeightPoint;
                short maxHeightPoint;
                short aNeighbourIdx = nodes.getNeighbour(aPointIdx);
                while ((aNeighbourIdx = neighbours.getNextNeighbour(aNeighbourIdx)) != 0){
                    short neighbourIdx = aNeighbourIdx;
                    short pointIndexNeighbour = neighbours.getNeighbourPoint(aNeighbourIdx);
                    if (nodes.isFlag(pointIndexNeighbour, BNodes.FLAG_VISITED)) continue; // this path is already handled
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
                        nodes.setFlag(pointIndexNeighbour, BNodes.FLAG_VISITED, true);
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
                            nodes.setFlag(pointIndexNeighbour, BNodes.FLAG_HEIGHT_RELEVANT, true);
                            if ( maxHeight == neighbourNodeEle ){
                                signumHeightInterval = 1;
                                if (minHeightPoint != lastHeightRelevantPoint){
                                    if (!nodes.isFlag(lastHeightRelevantPoint, BNodes.FLAG_FIX)){
                                        nodes.setFlag(minHeightPoint, BNodes.FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, minHeightPointIdx) <  PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( nodes.getEle(minHeightPoint) - nodes.getEle(lastHeightRelevantPoint) ) ) nodes.setFlag(lastHeightRelevantPoint, BNodes.FLAG_HEIGHT_RELEVANT, false); // reset height relevance
                                        }
                                    }

                                }
                            }
                            if ( minHeight == neighbourNodeEle ){
                                signumHeightInterval = -1;
                                if (maxHeightPoint != lastHeightRelevantPoint){
                                    if (!nodes.isFlag(lastHeightRelevantPoint,BNodes.FLAG_FIX)) {
                                        nodes.setFlag(maxHeightPoint, BNodes.FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, maxHeightPointIdx) < PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( nodes.getEle(maxHeightPoint) - nodes.getEle(lastHeightRelevantPoint) ) ) nodes.setFlag(lastHeightRelevantPoint, BNodes.FLAG_HEIGHT_RELEVANT, false); // reset height relevance
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
                        if (nodes.isFlag(pointIndexNeighbour, BNodes.FLAG_FIX)) break; // main exit from loop!

                        neighbourIdx = oppositeNeighbour(pointIndexNeighbour, neighbours.getReverse(neighbourIdx));
                        pointIndexNeighbour = neighbours.getNeighbourPoint(neighbourIdx);
                    } // while true
                    if (!nodes.isFlag(lastHeightRelevantPoint, BNodes.FLAG_FIX) &&
                            (distance (smoothNeighbourList,  lastHeightRelevantPointIdx, neighbourNodeIdx) < PointModelUtil.getCloseThreshold()/2d)){
                        nodes.setFlag(lastHeightRelevantPoint, BNodes.FLAG_HEIGHT_RELEVANT, false); // reset last heightRelevantPoint in this segment - otherwise the remaining
                    }

                    mgLog.v(()->neighbours.getNeighbourPoint(smoothNeighbourList.get(0))+" --- "+neighbours.getNeighbourPoint(smoothNeighbourList.get(smoothNeighbourList.size()-1))+" ("+smoothNeighbourList.size()+")");

                    if (smoothNeighbourList.size() <= 2) continue;

                    int startIdx = 0;
                    while (startIdx < smoothNeighbourList.size() -1){
                        int endIdx = startIdx;
                        while ((startIdx == endIdx) || !nodes.isFlag( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)), BNodes.FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                        }
                        float startHeight = nodes.getEle( neighbours.getNeighbourPoint( smoothNeighbourList.get(startIdx)) );
                        float endHeight = nodes.getEle( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)) );
                        double totalDistance = distance(smoothNeighbourList, startIdx, endIdx);

                        endIdx = startIdx;
                        double distance = 0;
                        while ((startIdx == endIdx) || !nodes.isFlag( neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx)), BNodes.FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                            short endNode = neighbours.getNeighbourPoint( smoothNeighbourList.get(endIdx));
                            if (!nodes.isFlag( endNode, BNodes.FLAG_HEIGHT_RELEVANT)){
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

}