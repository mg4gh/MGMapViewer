package mg.mgmap.generic.graph.impl3;

import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;


import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class DGraphTile implements Graph {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    /* ******************************************************************************************** */
    /* **************************************** Node stuff **************************************** */
    /* ******************************************************************************************** */

    static int NODE_SIZE = 16;

    static int ND_LA_OFFSET = 0;
    static int ND_LO_OFFSET = 2;
    static int ND_ELE_OFFSET = 4;
    static int ND_ELEACC_OFFSET = 8;
    static int ND_FLAGS_OFFSET = 12;
    static int ND_BORDER_OFFSET = 13;
    static int ND_NEIGHBOUR_OFFSET = 14;

    static byte ND_FLAG_VALID = 0x01;

    short addNode(short dLa, short dLo, float ele, float eleAcc, byte flags, byte border){
        int pos = nodesBuffer.position();
        nodesBuffer.putShort(dLa);
        nodesBuffer.putShort(dLo);
        nodesBuffer.putFloat(ele);
        nodesBuffer.putFloat(eleAcc);
        nodesBuffer.put(flags);
        nodesBuffer.put(border);
        nodesBuffer.putShort((short)0); // neighbourIdx not yet set
        return (short)(pos/NODE_SIZE);
    }

    @SuppressWarnings("SameParameterValue")
    void addNodeAt(short nodeIdx, short dLa, short dLo, float ele, float eleAcc, byte flags, byte border){
        nodesBuffer.position(NODE_SIZE * nodeIdx);
        addNode(dLa, dLo, ele, eleAcc, flags, border);
    }

    DNode getNode(short nodeIdx){
        nodesBuffer.position(NODE_SIZE * nodeIdx);
        return new DNode(tileIdx,nodeIdx,minLa+nodesBuffer.getShort(),minLo+nodesBuffer.getShort(),nodesBuffer.getFloat());
    }
    DNode getNodeData(DNode node, short nodeIdx){
        nodesBuffer.position(NODE_SIZE * nodeIdx);
        node.tileIdx = tileIdx;
        node.nodeIdx = nodeIdx;
        node.setLa(minLa+nodesBuffer.getShort());
        node.setLo(minLo+nodesBuffer.getShort());
        node.setEle(nodesBuffer.getFloat());
        return node;
    }

    double getNdLat(short nodeIdx){
        return LaLo.md2d(minLa + nodesBuffer.getShort(NODE_SIZE * nodeIdx + ND_LA_OFFSET));
    }
    double getNdLon(short nodeIdx){
        return LaLo.md2d(minLo + nodesBuffer.getShort(NODE_SIZE * nodeIdx + ND_LO_OFFSET));
    }
    float getNdEle(short nodeIdx){
        return nodesBuffer.getFloat(NODE_SIZE * nodeIdx + ND_ELE_OFFSET);
    }
    @SuppressWarnings("unused")
    float getNdEleAcc(short nodeIdx){
        return nodesBuffer.getFloat(NODE_SIZE * nodeIdx + ND_ELEACC_OFFSET);
    }
    byte getNdFlags(short nodeIdx){
        return nodesBuffer.get(NODE_SIZE * nodeIdx + ND_FLAGS_OFFSET);
    }
    @SuppressWarnings("SameParameterValue")
    void setNdFlags(short nodeIdx, byte flags){
        nodesBuffer.put(NODE_SIZE * nodeIdx + ND_FLAGS_OFFSET, flags);
    }
    byte getNdBorder(short nodeIdx){
        return nodesBuffer.get(NODE_SIZE * nodeIdx + ND_BORDER_OFFSET);
    }
    short getNdNeighbourIdx(short nodeIdx){
        return nodesBuffer.getShort(NODE_SIZE * nodeIdx + ND_NEIGHBOUR_OFFSET);
    }
    void setNdNeighbourIdx(short nodeIdx, short neighbourIdx){
        nodesBuffer.putShort(NODE_SIZE * nodeIdx + ND_NEIGHBOUR_OFFSET, neighbourIdx);
    }


    /* ******************************************************************************************** */
    /* ************************************* Neighbour stuff ************************************** */
    /* ******************************************************************************************** */

    static int NEIGHBOUR_SIZE = 14;

    static int NB_NEIGHBOUR_NODE_OFFSET = 0;
    static int NB_NEXT_NEIGHBOUR_OFFSET = 2;
    static int NB_REVERSE_OFFSET = 4;
    static int NB_WAY_ATTRIBUTES_OFFSET = 6;
    static int NB_COST_OFFSET = 8;
    static int NB_BORDER_OFFSET = 12;
    static int NB_FLAGS_OFFSET = 13;

    static byte NB_FLAG_VALID = 0x01;
    static byte NB_FLAG_PRIMARY_DIRECTION = 0x02;

    @SuppressWarnings("SameParameterValue")
    short addNeighbour(short neighbourNodeIdx, short nextNeighbourIdx, short reverseIdx, short wayAttributesIdx, float cost, byte border, byte flags){
        int pos = nodesBuffer.position();
        neighboursBuffer.putShort(neighbourNodeIdx);
        neighboursBuffer.putShort(nextNeighbourIdx);
        neighboursBuffer.putShort(reverseIdx);
        neighboursBuffer.putShort(wayAttributesIdx);
        neighboursBuffer.putFloat(cost);
        neighboursBuffer.put(border);
        neighboursBuffer.put(flags);
        return (short)(pos/NEIGHBOUR_SIZE);
    }

    @SuppressWarnings("SameParameterValue")
    void addNeighbourAt(short neighbourIdx, short neighbourNodeIdx, short nextNeighbourIdx, short reverseIdx, short wayAttributesIdx, byte border, byte flags){
        neighboursBuffer.position(neighbourIdx * NEIGHBOUR_SIZE);
        addNeighbour(neighbourNodeIdx, nextNeighbourIdx, reverseIdx, wayAttributesIdx, -1f, border, flags);
    }

    short getNbNeighbourNodeIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_NEIGHBOUR_NODE_OFFSET);
    }
    short getNbNextNeighbourIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_NEXT_NEIGHBOUR_OFFSET);
    }
    void setNbNextNeighbourIdx(short neighbourIdx, short nextNeighbourIdx){
        neighboursBuffer.putShort(NEIGHBOUR_SIZE * neighbourIdx + NB_NEXT_NEIGHBOUR_OFFSET, nextNeighbourIdx);
    }
    short getNbReverseIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_REVERSE_OFFSET);
    }
//    void setNbReverseIdx(short neighbourIdx, short reverseIdx){
//        neighboursBuffer.putShort(NEIGHBOUR_SIZE * neighbourIdx + NB_REVERSE_OFFSET, reverseIdx);
//    }
    short getNbWayAttributesIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_WAY_ATTRIBUTES_OFFSET);
    }
    float getNbCost(short neighbourIdx){
        return neighboursBuffer.getFloat(NEIGHBOUR_SIZE * neighbourIdx + NB_COST_OFFSET);
    }
    @SuppressWarnings("SameParameterValue")
    void setNbCost(short neighbourIdx, float cost){
        neighboursBuffer.putFloat(NEIGHBOUR_SIZE * neighbourIdx + NB_COST_OFFSET, cost);
    }
    byte getNbBorder(short neighbourIdx){
        return neighboursBuffer.get(NEIGHBOUR_SIZE * neighbourIdx + NB_BORDER_OFFSET);
    }
    byte getNbFlags(short neighbourIdx){
        return neighboursBuffer.get(NEIGHBOUR_SIZE * neighbourIdx + NB_FLAGS_OFFSET);
    }
    boolean isNbValid(short neighbourIdx){
        return (neighboursBuffer.get(NEIGHBOUR_SIZE * neighbourIdx + NB_FLAGS_OFFSET) & NB_FLAG_VALID) == NB_FLAG_VALID;
    }
    boolean isNbPrimaryDirection(short neighbourIdx){
        return (neighboursBuffer.get(NEIGHBOUR_SIZE * neighbourIdx + NB_FLAGS_OFFSET) & NB_FLAG_PRIMARY_DIRECTION) == NB_FLAG_PRIMARY_DIRECTION;
    }
    @SuppressWarnings("SameParameterValue")
    void setNbFlags(short neighbourIdx, byte flags){
        neighboursBuffer.put(NEIGHBOUR_SIZE * neighbourIdx + NB_FLAGS_OFFSET, flags);
    }



    /* ******************************************************************************************** */
    /* ************************************ DGraphTile stuff ************************************** */
    /* ******************************************************************************************** */


    static ByteBuffer initialNodesBuffer = ByteBuffer.allocateDirect(Config.MAX_NODE_CNT * NODE_SIZE);
    static ByteBuffer initialNeighboursBuffer = ByteBuffer.allocateDirect(Config.MAX_NEIGHBOUR_CNT * NEIGHBOUR_SIZE);
    static TreeMap<Integer, Short> initialNodeIdxMap = new TreeMap<>();

    private final ElevationProvider elevationProvider;
    final Tile tile;
    final int tileIdx;
    final BBox bBox;
    final int minLa,minLo,maxLa,maxLo;

    final ArrayList<WayAttributs> wayAttributesList = new ArrayList<>();
    short initialNodeCnt; // number of nodes after setup from WayProvider  - no approach node
    short maxNodeCnt;
    ByteBuffer nodesBuffer;
    short initialNeighbourCnt; // number of neighbours after setup from WayProvider  - no approach neighbours, no tile connect neighbours
    short maxNeighbourCnt;
    ByteBuffer neighboursBuffer;
    short borderNodeCnt = 0;
    final DGraphTile[] neighbourTiles = new DGraphTile[Border.BORDER_NODE_WEST+1];//use BORDER constants as index, although some entries stay always null
    boolean used = false;
    // TODO replace node ref arrays with a two dimentional array
    DNodeRef[] nodeRefs = null;
    DNodeRef[] reverseNodeRefs = null;

    private final WriteablePointModel hgtTemp = new WriteablePointModelImpl();
    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    long accessTime = 0; // used for cache

    public DGraphTile(ElevationProvider elevationProvider, Tile tile){
        this.elevationProvider = elevationProvider;
        this.tile = tile;
        tileIdx = DGraphTileFactory.getTileIdx(tile.tileX, tile.tileY);

        bBox = BBox.fromBoundingBox(this.tile.getBoundingBox());
        minLa = LaLo.d2md(bBox.minLatitude);
        minLo = LaLo.d2md(bBox.minLongitude);
        maxLa = LaLo.d2md(bBox.maxLatitude);
        maxLo = LaLo.d2md(bBox.maxLongitude);

        // reset static buffers for initialization of new DGraphTile object
        initialNodesBuffer.reset();
        initialNeighboursBuffer.reset();
        while (initialNodesBuffer.hasRemaining()) initialNodesBuffer.put((byte)0);
        while (initialNeighboursBuffer.hasRemaining()) initialNeighboursBuffer.put((byte)0);
        initialNodesBuffer.reset();
        initialNeighboursBuffer.reset();
        initialNodeIdxMap.clear();

        nodesBuffer = initialNodesBuffer;
        neighboursBuffer = initialNeighboursBuffer;
        mgLog.d("created: "+this);
    }

    public void addLatLongs(WayAttributs wayAttributes, LatLong[] latLongs) {
        short wayAttributesIdx = (short)wayAttributesList.size();
        wayAttributesList.add(wayAttributes);

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

    public void setupFinished(){
        int initialNodeCnt = nodesBuffer.position() / NODE_SIZE;
        int initialNeighbourCnt = neighboursBuffer.position() / NEIGHBOUR_SIZE;

        maxNodeCnt = (short)(initialNodeCnt + 2); // add 2 for temporary sourceApproachNode and targetApproachNode
        nodesBuffer = ByteBuffer.allocateDirect(maxNodeCnt * NODE_SIZE);
        System.arraycopy(initialNodesBuffer.array(), 0, nodesBuffer.array(), 0, initialNodeCnt * NODE_SIZE);

        maxNeighbourCnt = (short)(initialNeighbourCnt+borderNodeCnt*2 +6); // add 2 neighbours per border node + 6 neighbours for temporary connection to sourceApproachNode and targetApproachNode
        neighboursBuffer = ByteBuffer.allocateDirect((initialNeighbourCnt+borderNodeCnt*2 +6) * NEIGHBOUR_SIZE);
        System.arraycopy(initialNeighboursBuffer.array(), 0, neighboursBuffer.array(), 0, initialNeighbourCnt * NEIGHBOUR_SIZE);

        nodeRefs = new DNodeRef[maxNodeCnt]; // use nodeIdx
        reverseNodeRefs = new DNodeRef[maxNodeCnt]; // use nodeIdx
    }

    void addSegment(short wayAttributesIdx, double lat1, double long1, double lat2, double long2){
        short nodeIdx1 = getAddNode( lat1, long1);
        short nodeIdx2 = getAddNode( lat2, long2);
        addSegment(wayAttributesIdx, nodeIdx1, nodeIdx2, true);
    }

    short getAddNode(double latitude, double longitude){
        int la = LaLo.d2md(latitude);
        int lo = LaLo.d2md(longitude);
        short dLa = (short) (la - bBox.minLatitude);
        short dLo = (short) (lo - bBox.minLongitude);
        int key = (la << 16) + lo;
        short expectedNodeIdx = (short)(initialNodesBuffer.position() / NODE_SIZE);
        Short oldNodeIdx = initialNodeIdxMap.putIfAbsent(key, expectedNodeIdx);

        if (oldNodeIdx == null){ // node is not yet existing
            hgtTemp.setLat(latitude);
            hgtTemp.setLon(longitude);
            elevationProvider.setElevation(hgtTemp);
            byte border = 0;
            if (dLa == 0) border |= Border.BORDER_NODE_SOUTH;
            if (dLo == 0) border |= Border.BORDER_NODE_WEST;
            if (dLa == (maxLa-minLa)) border |= Border.BORDER_NODE_NORTH;
            if (dLo == (maxLo-minLo)) border |= Border.BORDER_NODE_EAST;
            short nodeIdx = addNode(dLa, dLo, hgtTemp.getEle(), hgtTemp.getEleAcc(), ND_FLAG_VALID, border);
            assert (nodeIdx == expectedNodeIdx);
            if (border != 0) borderNodeCnt++;
            return nodeIdx;
        } else {
            return oldNodeIdx;
        }
    }

    void addSegment(short wayAttributesIdx, short nodeIdx1, short nodeIdx2, boolean initPhase){
        short expectedNeighbourIdx = getExpectedNeighbourIdx(initPhase);

        byte border = 0;
        if (!initPhase) neighboursBuffer.position(expectedNeighbourIdx * NEIGHBOUR_SIZE);
        short neighbourIdx12 = addNeighbour(nodeIdx2, getNdNeighbourIdx(nodeIdx1), (short)(expectedNeighbourIdx+1), wayAttributesIdx, -1f, border, (byte)(NB_FLAG_VALID | NB_FLAG_PRIMARY_DIRECTION));
        short neighbourIdx21 = addNeighbour(nodeIdx1, getNdNeighbourIdx(nodeIdx2), expectedNeighbourIdx, wayAttributesIdx, -1f, border, NB_FLAG_VALID);
        setNdNeighbourIdx(nodeIdx1, neighbourIdx12);
        setNdNeighbourIdx(nodeIdx2, neighbourIdx21);
        assert (expectedNeighbourIdx == neighbourIdx12);
        assert (expectedNeighbourIdx+1 == neighbourIdx21);
    }






    short findFreeInterconnectNeighbour(){
        short neighbourIdx = initialNeighbourCnt; // all neighbours before are static
        while ((neighboursBuffer.get(neighbourIdx * NEIGHBOUR_SIZE + NB_FLAGS_OFFSET) & NB_FLAG_VALID) != 0){
            neighbourIdx++;
            assert (neighbourIdx < maxNeighbourCnt -6); // 6 neighbours are reserved for approach connectivity
        }
        return neighbourIdx;
    }



    public int countNeighbours(short nodeIdx){
        int cnt = 0;
        short neighbourIdx = getNdNeighbourIdx(nodeIdx);
        while (neighbourIdx != -1){
            cnt++;
            neighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
        }
        return cnt;
    }

    public void removeNeighbours(byte border) {
        removeNeighbours(true, border, (short)-1);
    }
    public void removeNeighbours(short neighbourNodeIdx) {
        removeNeighbours(false, (byte)0, neighbourNodeIdx);
    }

    /** scope - true: remove neighbours to neighbour tile based on border;
     *          false: remove all neighbours to neighbourNodeIdx (used for approachNode) */
    public void removeNeighbours(boolean scope, byte border, short neighbourNodeIdx){
        for (short nodeIdx=0; nodeIdx<maxNodeCnt; nodeIdx++){
            if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) != 0){ // node is valid
                if (scope?((getNdBorder(nodeIdx) & border) != 0):true){ // node is on relevant border
                    short lastNeighbourIdx = -1;
                    short neighbourIdx = getNdNeighbourIdx(nodeIdx);
                    while (neighbourIdx != -1){
                        short nextNeighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
                        if (scope?(getNbBorder(neighbourIdx) == border):(getNbNeighbourNodeIdx(neighbourIdx) == neighbourNodeIdx)){
                            setNbFlags(neighbourIdx, (byte)0); // set neighbour to invalid
                            if (lastNeighbourIdx == -1){
                                setNdNeighbourIdx(nodeIdx, nextNeighbourIdx);
                            } else {
                                setNbNextNeighbourIdx(lastNeighbourIdx, nextNeighbourIdx);
                            }
                        } else {
                            lastNeighbourIdx = neighbourIdx;
                        }
                        neighbourIdx = nextNeighbourIdx;
                    }
                }
            }
        }
    }

//    public void removeNeighbours(byte border){
//        for (short nodeIdx=0; nodeIdx<maxNodeCnt; nodeIdx++){
//            if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) != 0){ // node is valid
//                if ((getNdBorder(nodeIdx) & border) != 0){ // node is on relevant border
//                    short lastNeighbourIdx = -1;
//                    short neighbourIdx = getNdNeighbourIdx(nodeIdx);
//                    while (neighbourIdx != -1){
//                        short nextNeighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
//                        if (getNbBorder(neighbourIdx) == border){
//                            setNbFlags(neighbourIdx, (byte)0); // set neighbour to invalid
//                            if (lastNeighbourIdx == -1){
//                                setNdNeighbourIdx(nodeIdx, nextNeighbourIdx);
//                            } else {
//                                setNbNextNeighbourIdx(lastNeighbourIdx, nextNeighbourIdx);
//                            }
//                        } else {
//                            lastNeighbourIdx = neighbourIdx;
//                        }
//                        neighbourIdx = nextNeighbourIdx;
//                    }
//                }
//            }
//        }
//    }
//    public void removeNeighbours(short neighbourNodeIdx){
//        for (short nodeIdx=0; nodeIdx<maxNodeCnt; nodeIdx++){
//            if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) != 0){ // node is valid
//                if (true){ // node is on relevant border
//                    short lastNeighbourIdx = -1;
//                    short neighbourIdx = getNdNeighbourIdx(nodeIdx);
//                    while (neighbourIdx != -1){
//                        short nextNeighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
//                        if (getNbNeighbourNodeIdx(neighbourIdx) == neighbourNodeIdx){
//                            setNbFlags(neighbourIdx, (byte)0); // set neighbour to invalid
//                            if (lastNeighbourIdx == -1){
//                                setNdNeighbourIdx(nodeIdx, nextNeighbourIdx);
//                            } else {
//                                setNbNextNeighbourIdx(lastNeighbourIdx, nextNeighbourIdx);
//                            }
//                        } else {
//                            lastNeighbourIdx = neighbourIdx;
//                        }
//                        neighbourIdx = nextNeighbourIdx;
//                    }
//                }
//            }
//        }
//    }

    void resetNodeRefs(){
        for (int i=0; i<maxNodeCnt; i++){
            nodeRefs[i] = null;
            reverseNodeRefs[i] = null;
        }
    }

    void resetCosts(){
        for (short neighbourIdx = 0; neighbourIdx < maxNeighbourCnt; neighbourIdx++){
            setNbCost(neighbourIdx, -1); // reset cost - can be done even for invalid entries
        }
        for (int waIdx=0; waIdx<wayAttributesList.size(); waIdx++){
            wayAttributesList.get(waIdx).setDerivedData(null);
        }
    }


    @NonNull
    public String toString() {
        return "DGraphTile-("+tile.tileX+","+tile.tileY+")";
    }

    @Override
    public ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRefDetails(PointModel node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> resList) {
        throw new UnsupportedOperationException();
    }

    void bestApproach(PointModel pos, DApproach bestApproach, WriteablePointModel pmTempApproach) {
        for (short nodeIdx = 0; nodeIdx < initialNodeCnt; nodeIdx++) {
            short neighbourIdx = getNdNeighbourIdx(nodeIdx);
            double nodeLat = getNdLat(nodeIdx);
            double nodeLon = getNdLon(nodeIdx);
            float nodeEle = getNdEle(nodeIdx);
            while (neighbourIdx != -1) {
                if (((getNbFlags(neighbourIdx) & NB_FLAG_PRIMARY_DIRECTION) != 0) && (getNbBorder(neighbourIdx) == 0)) { // neighbour relations exist in both direction - here we can reduce to one; skip border neighbours
                    short neighbourNodeIdx = getNbNeighbourNodeIdx(neighbourIdx);
                    double neighbourNodeLat = getNdLat(neighbourNodeIdx);
                    double neighbourNodeLon = getNdLon(neighbourNodeIdx);
                    float neighbourNodeEle = getNdEle(neighbourNodeIdx);

                    if (PointModelUtil.findApproach(pos.getLat(), pos.getLon(),
                            nodeLat, nodeLon, nodeEle, 0L,
                            neighbourNodeLat, neighbourNodeLon, neighbourNodeEle, 0L,
                            pmTempApproach, PointModelUtil.getCloseThreshold())) {
                        float distance = (float) (PointModelUtil.distance(pos, pmTempApproach) + 0.0001);
                        if ((distance < PointModelUtil.getCloseThreshold()) && (distance < bestApproach.getApproachDistance())) { // ok, is close ==> new Approach found; and it's better than current best
                            bestApproach.tileIdx = tileIdx;
                            bestApproach.node1Idx = nodeIdx;
                            bestApproach.node2Idx = neighbourNodeIdx;
                            bestApproach.neighbour1to2 = neighbourIdx;
                            bestApproach.pos = pos;
                            bestApproach.approachNode.setLat(pmTempApproach.getLat());
                            bestApproach.approachNode.setLon(pmTempApproach.getLon());
                            bestApproach.approachNode.setEle(pmTempApproach.getEle());
                            bestApproach.distance = distance;
                        }
                    }
                }
                neighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
            }
        }
    }

    void connectApproach2Graph(DApproach dam){
        // there are two possible entries for approach nodes - idx initialNodeCnt and initialNodeCnt+1
        short nodeIdx = initialNodeCnt;
        if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) == ND_FLAG_VALID){
            nodeIdx++;
            if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) == ND_FLAG_VALID){
                throw new RuntimeException("ApproachNode indexes are in use.");
            }
        }
        short dLa = (short) (dam.approachNode.getLa() - bBox.minLatitude);
        short dLo = (short) (dam.approachNode.getLo() - bBox.minLongitude);
        addNodeAt(nodeIdx,dLa,dLo,dam.approachNode.getEle(),dam.approachNode.getEleAcc(),ND_FLAG_VALID,(byte)0);
        dam.approachNode.nodeIdx = nodeIdx; // save approach node index in approach

        short wayAttributesIdx = getNbWayAttributesIdx(dam.neighbour1to2);
        addSegment(wayAttributesIdx, dam.node1Idx, nodeIdx, false);
        addSegment(wayAttributesIdx, nodeIdx, dam.node2Idx, false);
    }


    short getExpectedNeighbourIdx(boolean initPhase){
        short expectedNeighbourIdx;
        if (initPhase) {
            expectedNeighbourIdx = (short)(initialNeighboursBuffer.position() / NEIGHBOUR_SIZE);
        } else {
            expectedNeighbourIdx = (short)(maxNeighbourCnt -6);
            while (((neighboursBuffer.get(expectedNeighbourIdx * NEIGHBOUR_SIZE + NB_FLAGS_OFFSET) & NB_FLAG_VALID) == NB_FLAG_VALID) && (expectedNeighbourIdx < maxNeighbourCnt)){
                expectedNeighbourIdx += 2;
            }
            if (expectedNeighbourIdx >= maxNeighbourCnt) throw new RuntimeException("ApproachNode neighbour indexes are in use.");
        }
        return expectedNeighbourIdx;
    }

    void checkDirectConnectApproaches(DApproach sdam, DApproach tdam){
        if (sdam.neighbour1to2 == tdam.neighbour1to2) { // ok, both approaches affect same neighbour connection (can't be reverse since approaches are setup only on primary direction)
            short wayAttributesIdx = getNbWayAttributesIdx(sdam.neighbour1to2);
            double node1Lat = getNdLat(sdam.node1Idx);
            double node1Lon = getNdLon(sdam.node1Idx);
            double distNode1Sdam = PointModelUtil.distance(node1Lat, node1Lon, sdam.approachNode.getLat(), sdam.approachNode.getLon());
            double distNode1Tdam = PointModelUtil.distance(node1Lat, node1Lon, sdam.approachNode.getLat(), sdam.approachNode.getLon());
            if (distNode1Sdam < distNode1Tdam) {
                addSegment(wayAttributesIdx, sdam.approachNode.nodeIdx, tdam.approachNode.nodeIdx, false);
            } else {
                addSegment(wayAttributesIdx, tdam.approachNode.nodeIdx, sdam.approachNode.nodeIdx, false);
            }
        }
    }

    void disconnectApproach2Graph(DApproach dam) {
        short nodeIdx = dam.approachNode.nodeIdx;
        setNdFlags(nodeIdx, (byte)0); // set node to invalid

        short neighbourIdx = getNdNeighbourIdx(nodeIdx);
        while (neighbourIdx != -1) {
            setNbFlags(neighbourIdx, (byte)0); // set neighbour to invalid
            neighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
        }
        removeNeighbours(nodeIdx);
        dam.approachNode.nodeIdx = -1;
    }

    @SuppressWarnings("ConstantConditions")
    void setupStatistic(AlgorithmStatistics statistics, ArrayList<PointModel> relaxedList){
        for (short nodeIdx=0; nodeIdx < maxNodeCnt; nodeIdx++){
            if ((getNdFlags(nodeIdx) & ND_FLAG_VALID) != 0){
                statistics.cntTotal++;
                if ((nodeRefs[nodeIdx] != null) || (reverseNodeRefs[nodeIdx] != null)){
                    statistics.cntRelaxed++;
                    // if (relaxedList != null) relaxedList.add(getNode(nodeIdx)); // !! generates lots of objects
                    if ((nodeRefs[nodeIdx].isSetteled()) && (reverseNodeRefs[nodeIdx].isSetteled())){
                        statistics.cntSettled++;
                    }
                }
            }
        }
    }

    DNodeRef getNodeRef(short nodeIdx, boolean reverse){
        if (reverse){
            return reverseNodeRefs[nodeIdx];
        } else {
            return nodeRefs[nodeIdx];
        }
    }
    void setNodeRef(short nodeIdx, boolean reverse, DNodeRef nodeRef){
        if (reverse){
            reverseNodeRefs[nodeIdx] = nodeRef;
        } else {
            nodeRefs[nodeIdx] = nodeRef;
        }
    }

}
