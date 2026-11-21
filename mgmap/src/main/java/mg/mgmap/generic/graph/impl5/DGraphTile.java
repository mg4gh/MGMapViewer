package mg.mgmap.generic.graph.impl5;

import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class DGraphTile implements Graph {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    /* ******************************************************************************************** */
    /* **************************************** Node stuff **************************************** */
    /* ******************************************************************************************** */

    static int NODE_SIZE = 20;

    static int ND_LA_OFFSET = 0;
    static int ND_LO_OFFSET = 2;
    static int ND_ELE_OFFSET = 4;
    @SuppressWarnings("unused")
    static int ND_ELEACC_OFFSET = 8;
    static int ND_FLAGS_OFFSET = 12;
    static int ND_BORDER_OFFSET = 13;
    static int ND_NEIGHBOUR_OFFSET = 14;
    static int ND_FW_REF_OFFSET = 16;
    static int ND_RV_REF_OFFSET = 18;

    static byte ND_FLAG_VALID = 0x01;
    static byte ND_FLAG_FIX = 0x02;
    static byte ND_FLAG_VISITED = 0x04;
    static byte ND_FLAG_HEIGHT_RELEVANT = 0x08;

    static short ND_DNODEREF_MARKER = (short)(0x8000);

    void addNode(short dLa, short dLo, float ele, float eleAcc, byte flags, byte border, short neighbourIdx){
        nodesBuffer.putShort(dLa);
        nodesBuffer.putShort(dLo);
        nodesBuffer.putFloat(ele);
        nodesBuffer.putFloat(eleAcc);
        nodesBuffer.put(flags);
        nodesBuffer.put(border);
        nodesBuffer.putShort(neighbourIdx);
        nodesBuffer.putInt(-1); // resets fw and rv nodeRef
    }

    String nodeToString(short nodeIdx){
        ByteBuffer nodesBuffer = (Thread.currentThread().getName().equals("routing"))?this.nodesBuffer:nodesBufferClone;
        nodesBuffer.position( nodeIdx * NODE_SIZE);
        return String.format(Locale.ENGLISH, "nodeIdx=%04d lat=%02.6f lon=%02.6f ele=%+05.2f eleAcc=%+05.1f flags=%02x border=%02x neighbourIdx=%05d  ref=%04X revRef=%04X ",
                nodeIdx, LaLo.md2d(minLa+nodesBuffer.getShort()), LaLo.md2d(minLo+nodesBuffer.getShort()), nodesBuffer.getFloat(), nodesBuffer.getFloat(), nodesBuffer.get(), nodesBuffer.get(), nodesBuffer.getShort(), nodesBuffer.getShort(), nodesBuffer.getShort());
    }

    DNode getNode(short nodeIdx){
        ByteBuffer nodesBuffer = (Thread.currentThread().getName().equals("routing"))?this.nodesBuffer:nodesBufferClone;
        nodesBuffer.position(NODE_SIZE * nodeIdx);
        return new DNode(tileIdx,nodeIdx,minLa+nodesBuffer.getShort(),minLo+nodesBuffer.getShort(),nodesBuffer.getFloat(),nodesBuffer.getFloat());
    }
    void getNodeData(DNode node, short nodeIdx){
        ByteBuffer nodesBuffer = (Thread.currentThread().getName().equals("routing"))?this.nodesBuffer:nodesBufferClone;
        nodesBuffer.position(NODE_SIZE * nodeIdx);
        node.tileIdx = tileIdx;
        node.nodeIdx = nodeIdx;
        node.setLa(minLa+nodesBuffer.getShort());
        node.setLo(minLo+nodesBuffer.getShort());
        node.setEle(nodesBuffer.getFloat());
        node.setEleAcc(nodesBuffer.getFloat());
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
    boolean isNdValid(short nodeIdx){
        return (nodesBuffer.get(NODE_SIZE * nodeIdx + ND_FLAGS_OFFSET) & ND_FLAG_VALID) == ND_FLAG_VALID;
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
    void setNdResetNodeRefs(short nodeIdx){
        nodesBuffer.putInt(NODE_SIZE * nodeIdx + ND_FW_REF_OFFSET, -1); // resets fw and rv nodeRef
    }
    static short eRefIdx(short refIdx){
        assert ((refIdx & ND_DNODEREF_MARKER) != 0);
        return (short)(refIdx & ~ND_DNODEREF_MARKER);
    }
    short getNdRef(short nodeIdx, boolean reverse){
        return nodesBuffer.getShort(NODE_SIZE *nodeIdx + (reverse?ND_RV_REF_OFFSET:ND_FW_REF_OFFSET));
    }

    void setNdRef(short nodeIdx, boolean reverse, short refIdx){
        nodesBuffer.putShort(NODE_SIZE *nodeIdx + (reverse?ND_RV_REF_OFFSET:ND_FW_REF_OFFSET), refIdx);
    }



    /* ******************************************************************************************** */
    /* ************************************* Neighbour stuff ************************************** */
    /* ******************************************************************************************** */

    static int NEIGHBOUR_SIZE = 20;
    static int NEIGHBOURS_FOR_FIX_GRAPH = 4;

    static int NB_NEIGHBOUR_NODE_OFFSET = 0;
    static int NB_NEXT_NEIGHBOUR_OFFSET = 2;
    static int NB_WAY_ATTRIBUTES_OFFSET = 4;
    static int NB_BORDER_OFFSET = 6;
    static int NB_FLAGS_OFFSET = 7;
    static int NB_DISTANCE_OFFSET = 8;
    static int NB_VERT_DISTANCE_OFFSET = 12;
    static int NB_COST_OFFSET = 16;

    static byte NB_FLAG_VALID = 0x01;
    static byte NB_FLAG_PRIMARY_DIRECTION = 0x02;

    void addNeighbour(short neighbourNodeIdx, short nextNeighbourIdx, short wayAttributesIdx, byte border, byte flags, float distance, float vertDistance){
        neighboursBuffer.putShort(neighbourNodeIdx);
        neighboursBuffer.putShort(nextNeighbourIdx);
        neighboursBuffer.putShort(wayAttributesIdx);
        neighboursBuffer.put(border);
        neighboursBuffer.put(flags);
        neighboursBuffer.putFloat(distance);
        neighboursBuffer.putFloat(vertDistance);
        neighboursBuffer.putFloat(-1f);
    }

    String neighbourToString(short neighbourIdx){
        neighboursBuffer.position( neighbourIdx * NEIGHBOUR_SIZE);
        return String.format(Locale.ENGLISH, "neighbourIdx=%05d neighbourNodeIdx=%04d nextNeighbourIdx=%05d wayAttributesIdx=%03d border=%02x flags=%02x distance=%+09.5f  vertDistance=%+09.5f  cost=%+09.5f ",
                neighbourIdx, neighboursBuffer.getShort(), neighboursBuffer.getShort(), neighboursBuffer.getShort(), neighboursBuffer.get(), neighboursBuffer.get(), neighboursBuffer.getFloat(), neighboursBuffer.getFloat(), neighboursBuffer.getFloat());
    }


    @SuppressWarnings("SameParameterValue")
    void addNeighbourAt(short neighbourIdx, short neighbourNodeIdx, short nextNeighbourIdx, short wayAttributesIdx, byte border, byte flags, float distance, float vertDistance){
        neighboursBuffer.position(neighbourIdx * NEIGHBOUR_SIZE);
        addNeighbour(neighbourNodeIdx, nextNeighbourIdx, wayAttributesIdx, border, flags, distance, vertDistance);
    }

    short getNbNeighbourNodeIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_NEIGHBOUR_NODE_OFFSET);
    }
    short getNbNextNeighbourIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_NEXT_NEIGHBOUR_OFFSET);
    }
    short getNbReverseIdx(short neighbourIdx){
        return (short)(neighbourIdx ^ 0x01);
    }
    short getNbWayAttributesIdx(short neighbourIdx){
        return neighboursBuffer.getShort(NEIGHBOUR_SIZE * neighbourIdx + NB_WAY_ATTRIBUTES_OFFSET);
    }
    float getNbDistance(short neighbourIdx){
        return neighboursBuffer.getFloat(NEIGHBOUR_SIZE * neighbourIdx + NB_DISTANCE_OFFSET);
    }
    float getNbVertDistance(short neighbourIdx){
        return neighboursBuffer.getFloat(NEIGHBOUR_SIZE * neighbourIdx + NB_VERT_DISTANCE_OFFSET);
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

    /* ******************************************************************************************** */
    /* ************************************ DGraphTile stuff ************************************** */
    /* ******************************************************************************************** */


    static ExDNode[] eNodes = null;
    static ExDNeighbour[] eNeighbours = null;
    static DNodeRef[] eRefs = null;

    static synchronized void initStatics(){
        if (eNodes == null){
            eNodes = new ExDNode[Config.MAX_NODE_CNT];
            for (short i = 0; i< Config.MAX_NODE_CNT; i++){
                if (eNodes[i] == null) eNodes[i] = new ExDNode(i);
            }
        }
        if (eNeighbours == null){
            eNeighbours = new ExDNeighbour[Config.MAX_NEIGHBOUR_CNT];
            for (short i = 0; i< Config.MAX_NEIGHBOUR_CNT; i++) {
                if (eNeighbours[i] == null) eNeighbours[i] = new ExDNeighbour(i);
            }
        }
        if (eRefs == null){
            eRefs = new DNodeRef[Config.MAX_DNODEREF_CNT];
            for (short i = 0; i< Config.MAX_DNODEREF_CNT; i++) {
                if (eRefs[i] == null) eRefs[i] = new DNodeRef();
            }
        }
    }


    final Tile tile;
    final ElevationProvider elevationProvider;
    final AlgorithmStatistics statistics;
    final double smoothingDistance; // negative value means no smoothing

    final int tileIdx;
    final BBox bBox;
    final int minLa,minLo,maxLa,maxLo;

    private final ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    final ArrayList<WayAttributs> wayAttributesList = new ArrayList<>();
    int initialNodeCnt; // number of nodes after setup from WayProvider  - no approach node
    int maxNodeCnt;
    ByteBuffer nodesBuffer;
    ByteBuffer nodesBufferClone;
    int initialNeighbourCnt; // number of neighbours after setup from WayProvider  - no approach neighbours, no tile connect neighbours
    int maxNeighbourCnt;
    ByteBuffer neighboursBuffer;
    ByteBuffer neighboursBufferClone;
    int borderNodeCnt = 0;

    TreeMap<ExDNode, ExDNode> initialNodeMap = new TreeMap<>();

    final DGraphTile[] neighbourTiles = new DGraphTile[Border.BORDER_NODE_WEST+1];//use BORDER constants as index, although some entries stay always null
    byte neighbourConnected = 0; // if e.g. neighbours on BORDER_NODE_WEST are added, then this bit is set in neighbourConnected

    boolean used = false;

    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    long creationNanos;
    long accessTime = 0; // used for cache
    HashMap <Long, Short> waValueIndexMap = null;

    public DGraphTile(Tile tile, ElevationProvider elevationProvider, AlgorithmStatistics statistics, double smoothingDistance){
        this.creationNanos = System.nanoTime();
        this.elevationProvider = elevationProvider;
        this.tile = tile;
        this.statistics = statistics;
        this.smoothingDistance = smoothingDistance;

        DGraphTile.initStatics();
        tileIdx = DGraphTileFactory.getTileIdx(tile.tileX, tile.tileY);

        bBox = BBox.fromBoundingBox(this.tile.getBoundingBox());
        minLa = LaLo.d2md(bBox.minLatitude);
        minLo = LaLo.d2md(bBox.minLongitude);
        maxLa = LaLo.d2md(bBox.maxLatitude);
        maxLo = LaLo.d2md(bBox.maxLongitude);

        initialNodeCnt = 0;
        initialNeighbourCnt = 0;
        neighbourTiles[0] = this;

        mgLog.d("created: "+this);
    }

    public void setupStarted(){
        waValueIndexMap = new HashMap<>();
    }

    public void addLatLongs(WayAttributs wayAttributes, LatLong[] latLongs) {
        Short wayAttributesIdx = waValueIndexMap.get(wayAttributes.getWaValue());
        if (wayAttributesIdx == null){
            wayAttributesIdx = (short)wayAttributesList.size();
            waValueIndexMap.put(wayAttributes.getWaValue(), wayAttributesIdx);
            wayAttributesList.add(wayAttributes);
        }

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
        waValueIndexMap = null;
        fixGraph();
        smooth(smoothingDistance);

        maxNodeCnt = initialNodeCnt;
        nodesBuffer = ByteBuffer.allocateDirect(maxNodeCnt * NODE_SIZE);
        for (int i=0; i<initialNodeCnt; i++){
            ExDNode node = eNodes[i];
            addNode(node.dla, node.dlo, node.getEle(), node.getEleAcc(), (byte)(node.flags & ND_FLAG_VALID), node.border, node.neighbour==null?-1:node.neighbour.neighbourIdx);
            node.neighbour = null;
        }
        nodesBufferClone = nodesBuffer.duplicate();

        maxNeighbourCnt = (short)(initialNeighbourCnt+borderNodeCnt*3 + NEIGHBOURS_FOR_FIX_GRAPH); // one border node should have in avg not more than 1.5 neighbourNodes in the neighbour tile
        neighboursBuffer = ByteBuffer.allocateDirect(maxNeighbourCnt * NEIGHBOUR_SIZE);
        for (int i=0; i<initialNeighbourCnt; i++) {
            ExDNeighbour neighbour = eNeighbours[i];
            addNeighbour(neighbour.neighbourNode.nodeIdx, neighbour.getNextNeighbourIdx(), neighbour.wayAttributesIdx, neighbour.border, neighbour.flags, neighbour.distance, neighbour.vertDistance);
            neighbour.neighbourNode = null;
            neighbour.nextNeighbour = null;
        }
        neighboursBufferClone = neighboursBuffer.duplicate();

        initialNodeMap.clear();
        statistics.setupNanos += (System.nanoTime() - creationNanos);
    }

    void addSegment(short wayAttributesIdx, double lat1, double long1, double lat2, double long2){
        ExDNode node1 = getAddNode( lat1, long1);
        ExDNode node2 = getAddNode( lat2, long2);
        addSegment(wayAttributesIdx, node1, node2);
    }

    ExDNode getAddNode(double latitude, double longitude){
        int la = LaLo.d2md(latitude);
        int lo = LaLo.d2md(longitude);

        int expectedNodeIdx = initialNodeCnt;
        ExDNode node = eNodes[expectedNodeIdx];
        node.init(la, lo, minLa, minLo);
        ExDNode mapNode = initialNodeMap.putIfAbsent(node,node);
        if (mapNode == null){ // node is not yet existing
            elevationProvider.setElevation(node);
            if (la == minLa) node.border|= Border.BORDER_NODE_SOUTH;
            if (lo == minLo) node.border|= Border.BORDER_NODE_WEST;
            if (la == maxLa) node.border|= Border.BORDER_NODE_NORTH;
            if (lo == maxLo) node.border|= Border.BORDER_NODE_EAST;
            if (node.border != 0) borderNodeCnt++;
            initialNodeCnt++;
            return node;
        } else {
            return mapNode;
        }
    }

    void addSegment(short wayAttributesIdx, ExDNode node1, ExDNode node2){  // during init phase
        int expectedNeighbourIdx = initialNeighbourCnt;
        ExDNeighbour neighbour12 = eNeighbours[expectedNeighbourIdx];
        ExDNeighbour neighbour21 = eNeighbours[expectedNeighbourIdx+1];
        float distance = (float)PointModelUtil.distance(node1,node2);
        float vertDistance = PointModelUtil.verticalDistance(node1,node2);
        neighbour12.init(node2, node1.neighbour, neighbour21, wayAttributesIdx, distance, vertDistance, (byte)(NB_FLAG_VALID | NB_FLAG_PRIMARY_DIRECTION));
        neighbour21.init(node1, node2.neighbour, neighbour12, wayAttributesIdx, distance, -vertDistance, NB_FLAG_VALID);
        node1.neighbour = neighbour12;
        node2.neighbour = neighbour21;
        node1.neighbourCnt++;
        node2.neighbourCnt++;
        initialNeighbourCnt += 2;
    }


    void fixGraph(){
        if (initialNodeCnt == 0) return;
        long startNanos = System.nanoTime();
        int laThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(Config.CONNECT_THRESHOLD_METER) );
        int loThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(Config.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );

        ExDNode iNode = null;
        while ( (iNode = (iNode==null)?initialNodeMap.firstKey():initialNodeMap.higherKey(iNode)) != null){

            if (iNode.flags == 0) continue; // iNode is invalid, take next

            ExDNode nNode = iNode;
            while ( (nNode = initialNodeMap.higherKey(nNode)) != null){
                if (nNode.flags == 0) continue; // nNode is invalid, take next

                assert (nNode.dla >= iNode.dla);
                if ((nNode.dla-iNode.dla) > laThreshold) break;
                if (Math.abs(nNode.dlo-iNode.dlo) > loThreshold) continue;
                if (PointModelUtil.distance(iNode, nNode) > Config.CONNECT_THRESHOLD_METER) continue;
                if (checkCommonWay(iNode, nNode)) continue; // points on the same way should not be connected

                if (nNode.border == 0){
                    reduceGraph(iNode, nNode); // drop nNode, replace neighbours with those from iNode
                } else if (iNode.border == 0){
                    reduceGraph(nNode, iNode); // drop iNode, replace neighbours with those from nNode
                } else { // cannot drop both nodes (since they are border nodes)
                    // ==> just connect; accept potential problem with routing hints
                    addSegment((short)-1, iNode, nNode); // neighbours are in the potential tile connect area - this should be large enough
                }
            }
        }
        statistics.fixNanos += (System.nanoTime() - startNanos);
    }

    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
    private void reduceGraph(ExDNode iNode, ExDNode nNode){
        mgLog.v("reduceGraph: "+nNode+"("+nNode.nodeIdx+") -> "+iNode+"("+iNode.nodeIdx+")");
        //determine last neighbour from iIdx
        ExDNeighbour lastNeighbour = iNode.neighbour; // assume that iNode has at least one neighbour (so is not null)
        while (lastNeighbour.nextNeighbour != null){
            lastNeighbour = lastNeighbour.nextNeighbour;
        }
        // now append all neighbours form nNode to iNode
        lastNeighbour.nextNeighbour = nNode.neighbour;
        // now iterate over all neighbours from nNode
        ExDNeighbour neighbour = nNode.neighbour;
        while (neighbour != null){
            // and set iNode as a Neighbour in the reverse neighbour
            neighbour.reverseNeighbour.neighbourNode = iNode;
            // fix also the distance entry for the neighbour and the reverse neighbour
            float distance = (float)PointModelUtil.distance(iNode, neighbour.neighbourNode);
            neighbour.distance = distance;
            neighbour.reverseNeighbour.distance = distance;
            float vertDistance = PointModelUtil.verticalDistance(iNode, neighbour.neighbourNode);
            neighbour.vertDistance = vertDistance;
            neighbour.reverseNeighbour.vertDistance = -vertDistance;
            // and move to next neighbour
            neighbour = neighbour.nextNeighbour;
            iNode.neighbourCnt++;
            nNode.neighbourCnt--;
        }
        // finally set nIdx to invalid
        nNode.flags = 0;
        nNode.neighbour = null; // not necessary, but for defensive approach
        assert (nNode.neighbourCnt == 0);
    }

    boolean checkCommonWay(ExDNode node1, ExDNode node2){
        for (ExDNeighbour neighbour1 = node1.neighbour; neighbour1 != null; neighbour1 = neighbour1.nextNeighbour){
            for (ExDNeighbour neighbour2 = node2.neighbour; neighbour2 != null; neighbour2 = neighbour2.nextNeighbour){
                if (neighbour1.wayAttributesIdx == neighbour2.wayAttributesIdx) return true;
            }
        }
        return false;
    }

    short findFreeInterconnectNeighbour(){
        short neighbourIdx = (short)initialNeighbourCnt; // all neighbours before are static
        while (isNbValid (neighbourIdx)){
            neighbourIdx += 2;
            assert (neighbourIdx < maxNeighbourCnt);
        }
        return neighbourIdx;
    }

    void resetNodeRefs(){
        for (short i=0; i<maxNodeCnt; i++){
            setNdResetNodeRefs(i);
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
        return "DGraphTile-("+tile.tileX+","+tile.tileY+") "+tileIdx;
    }

    @Override
    public ArrayList<PointModel> segmentNodes(ApproachModel approachModel) {
        ArrayList<PointModel> resList = new ArrayList<>();
        if (approachModel instanceof DApproach dam) {
            assert (dam.tileIdx == tileIdx);
            resList.add(getNode(dam.node1Idx));
            resList.add(getNode(dam.node2Idx));
            short neighbourIdx =  getNbReverseIdx( dam.neighbour1to2 );
            short nodeIdx = dam.node2Idx;
            if (getNdBorder(nodeIdx) == 0){
                while ( (neighbourIdx = oppositeNeighbour(nodeIdx, neighbourIdx)) != -1){
                    nodeIdx = getNbNeighbourNodeIdx(neighbourIdx);
                    resList.add(getNode(nodeIdx));
                    if (getNdBorder(nodeIdx) != 0) break;
                    neighbourIdx = getNbReverseIdx(neighbourIdx);
                }
            }
            neighbourIdx = dam.neighbour1to2;
            nodeIdx = dam.node1Idx;
            if (getNdBorder(nodeIdx) == 0) {
                while ((neighbourIdx = oppositeNeighbour(nodeIdx, neighbourIdx)) != -1) {
                    nodeIdx = getNbNeighbourNodeIdx(neighbourIdx);
                    resList.add(0, getNode(nodeIdx));
                    if (getNdBorder(nodeIdx) != 0) break;
                    neighbourIdx = getNbReverseIdx(neighbourIdx);
                }
            }
        }
        return resList;
    }

    @Override
    public BBox getBBox(){
        return bBox;
    }

    @Override
    public String getRefDetails(PointModel node) {
        if (node instanceof DNode dNode) return " nodeIdx="+dNode.nodeIdx;
        return "";
    }

    @Override
    public ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> resList) {
        throw new UnsupportedOperationException();
    }

    short findNeighbourIdx(short nodeIdx, byte border, DNode neighbourNode) {
        short neighbourIdx = getNdNeighbourIdx(nodeIdx);
        while (neighbourIdx != -1) {
            if (getNbBorder(neighbourIdx) == border) {
                short neighbourNodeIdx = getNbNeighbourNodeIdx(neighbourIdx);
                if ((neighbourTiles[border].getNdLat(neighbourNodeIdx) == neighbourNode.getLat()) && (neighbourTiles[border].getNdLon(neighbourNodeIdx) == neighbourNode.getLon())) {
                    return neighbourIdx;
                }
            }
            neighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
        }
        return -1;
    }

    void bestApproach(PointModel pos, DApproach bestApproach, WriteablePointModel pmTempApproach, int closeThreshold) {
        for (short nodeIdx = 0; nodeIdx < initialNodeCnt; nodeIdx++) {
            if (!isNdValid(nodeIdx)) continue;
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
                            pmTempApproach, closeThreshold)) {
                        float distance = (float) (PointModelUtil.distance(pos, pmTempApproach) + 0.0001);
                        if ((distance < closeThreshold) && (distance < bestApproach.getApproachDistance())) { // okay, is close ==> new Approach found; and it's better than current best
                            bestApproach.tileIdx = tileIdx;
                            bestApproach.node1Idx = nodeIdx;
                            bestApproach.node2Idx = neighbourNodeIdx;
                            bestApproach.neighbour1to2 = neighbourIdx;
                            bestApproach.pos = pos;
                            bestApproach.approachNode.tileIdx  = tileIdx;
                            bestApproach.approachNode.setLat(pmTempApproach.getLat());
                            bestApproach.approachNode.setLon(pmTempApproach.getLon());
                            bestApproach.approachNode.setEle(Math.round(pmTempApproach.getEle()*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR);
                            bestApproach.distance = distance;
                        }
                    }
                }
                neighbourIdx = getNbNextNeighbourIdx(neighbourIdx);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    void setupStatistic(AlgorithmStatistics statistics){
        for (short nodeIdx=0; nodeIdx < maxNodeCnt; nodeIdx++){
            if (isNdValid(nodeIdx)){
                short fwRef = getNdRef(nodeIdx, false);
                short rvRef = getNdRef(nodeIdx, true);
                statistics.cntTotal++;
                if ((fwRef != -1) || (rvRef != -1)) {
                    statistics.cntRelaxed++;
                    if ((fwRef >= 0) || (rvRef >= 0)) {
                        statistics.cntSettled++;
                    }
                }
                if (fwRef < -1){
                    statistics.dNodeRefCnt++;
                } else if (fwRef >= 0){
                    statistics.miDNodeRefCnt++;
                }
                if (rvRef < -1){
                    statistics.dNodeRefCnt++;
                } else if (rvRef >= 0){
                    statistics.miDNodeRefCnt++;
                }
            }
        }
    }

    private static short nextFreeERefIdx = 0;
    static short findFreeERefIdx(){
        short idx = nextFreeERefIdx;
        while (true){
            short nextIdx = (idx==Config.MAX_DNODEREF_CNT-1)?0:(short)(idx+1);
            if (eRefs[idx].isValid()) {
                idx = nextIdx;
                continue;
            }
            nextFreeERefIdx = nextIdx;
            return idx;
        }
    }

    void smooth(double smoothingDistance){
        if (smoothingDistance <= 0) return;
        long startNanos = System.nanoTime();
        // Mark all nodes with != 2 Neighbours and all border nodes as FIX (and therefore HIGHT_RELEVANT)
        for (short nodeIdx=0; nodeIdx<initialNodeCnt; nodeIdx++){
            ExDNode node = eNodes[nodeIdx];
            if (node.hasFlags(NB_FLAG_VALID)){ // process only valid nodes
                node.setFlags(ND_FLAG_FIX, ND_FLAG_HEIGHT_RELEVANT);
                if (node.border == 0){
                    if ((node.neighbourCnt == 2) && (node.neighbour.wayAttributesIdx == node.neighbour.nextNeighbour.wayAttributesIdx)){
                        node.unsetFlags(ND_FLAG_FIX, ND_FLAG_HEIGHT_RELEVANT);
                    }
                }
            }
        }

        // Iterate over FIX nodes
        ArrayList<ExDNeighbour> smoothList = new ArrayList<>(); // contains neighbourIdx values that point to Nodes that are on the VISITING process
        for (ExDNode node = initialNodeMap.firstKey(); node != null; node = initialNodeMap.higherKey(node)){
//        for (short nodeIdx=0; nodeIdx<initialNodeCnt; nodeIdx++){ // iterate over all nodes
//            ExDNode node = eNodes[nodeIdx];
            if (node.hasFlags(ND_FLAG_VALID, ND_FLAG_FIX)){ // node is valid and fix
                ExDNeighbour aNeighbour = null;
                while ( (aNeighbour = (aNeighbour==null)?node.neighbour:aNeighbour.nextNeighbour) != null){
                    ExDNeighbour neighbour = aNeighbour;

                    if (neighbour.neighbourNode.hasFlags(ND_FLAG_FIX ) || neighbour.neighbourNode.hasFlags(ND_FLAG_VISITED)){
                        continue; // VISITED -> path is already handled; FIX -> path cannot be smoothed
                    }
                    smoothList.clear();
                    smoothList.add(neighbour.reverseNeighbour); // a neighbour pointing to nodeIdx)

                    float minHeight = node.getEle();
                    float maxHeight = minHeight;
                    ExDNode lastHeightRelevantPoint = node;
                    float lastHeightRelevantPointEle = minHeight;
                    double distance2LastHeightRelevant = 0;

                    while (true) { // follow the neighbours until next FIX point - try to mark relevant points HEIGHT_RELEVANT; and all on the path as VISITED
                        smoothList.add(neighbour);
                        ExDNode neighbourNode = neighbour.neighbourNode;
                        distance2LastHeightRelevant += neighbour.distance;

                        if (neighbourNode.hasFlags(ND_FLAG_FIX))
                            break; // new FIX point reached, no more intermediate HEIGHT_RELEVANT points (don't leave loop before adding this point to smoothList)
                        neighbourNode.setFlags(ND_FLAG_VISITED);

                        float neighbourNodeEle = neighbourNode.getEle();
                        minHeight = Math.min(minHeight, neighbourNodeEle);
                        maxHeight = Math.max(maxHeight, neighbourNodeEle);

                        if ((distance2LastHeightRelevant > smoothingDistance * 5) ||
                                ((Math.abs(neighbourNodeEle - lastHeightRelevantPointEle) >= TrackLogStatistic.ELE_THRESHOLD_ELSE) && (distance2LastHeightRelevant > smoothingDistance))) {
                            neighbourNode.setFlags(ND_FLAG_HEIGHT_RELEVANT);
                            lastHeightRelevantPoint = neighbourNode;
                            lastHeightRelevantPointEle = neighbourNodeEle;
//                            lastHeightRelevantPointSLIdx = neighbourNodeSLIdx;
                            minHeight = neighbourNodeEle;
                            maxHeight = neighbourNodeEle;
                            distance2LastHeightRelevant = 0;
                        }

                        neighbour = oppositeNeighbour(neighbourNode, neighbour.reverseNeighbour);
                    } // while (true)

                    if (!lastHeightRelevantPoint.hasFlags(ND_FLAG_FIX) && (distance2LastHeightRelevant < smoothingDistance)){
                        lastHeightRelevantPoint.unsetFlags(ND_FLAG_HEIGHT_RELEVANT);
                    }
                    // now height relevant points are marked

                    if (smoothList.size() <= 2) continue;

                    // now follow again the neighbours; sequence is in the smoothList given
                    int startIdx = 0;
                    while (startIdx < smoothList.size() -1) {
                        // first identify a subset of neighbours till next HEIGHT_RELEVANT point
                        int endIdx = startIdx + 1;
                        double totalDistance = 0;
                        while (true) {
                            ExDNode endNode = smoothList.get(endIdx).neighbourNode;
                            totalDistance += smoothList.get(endIdx).distance;
                            if (endNode.hasFlags(ND_FLAG_HEIGHT_RELEVANT))
                                break; // if endIdx is pointing to a node that is height relevant
                            endIdx++;
                        }
                        float startHeight = smoothList.get(startIdx).neighbourNode.getEle();
                        float endHeight = smoothList.get(endIdx).neighbourNode.getEle();

                        // now to the real smoothing for this segment of the smoothList
                        endIdx = startIdx + 1;
                        double distance = 0;
                        while (true) {
                            ExDNeighbour endNeighbour = smoothList.get(endIdx);
                            ExDNode endNode = endNeighbour.neighbourNode;
                            if (endNode.hasFlags(ND_FLAG_HEIGHT_RELEVANT)) {
                                validateVertDistance(endNeighbour);
                                break; // if endIdx is pointing to a node that is height relevant
                            }
                            distance += smoothList.get(endIdx).distance;
                            float height = (float) PointModelUtil.interpolate(0, totalDistance, startHeight, endHeight, distance);
                            height = Math.round(height * PointModelUtil.ELE_FACTOR) / PointModelUtil.ELE_FACTOR;
                            endNode.setEle(height);
                            validateVertDistance(endNeighbour);
                            endIdx++;
                        }
                        startIdx = endIdx;
                    }

//                    neighbour = nextNeighbour;
                } // while (neighbourIdx != -1){ // ... and iterate over all its neighbours
            } // if node is valid and fix
        } // for (short nodeIdx=0; nodeIdx<initialNodeCnt; nodeIdx++){ // iterate over all nodes
        statistics.smoothNanos += (System.nanoTime() - startNanos);
    }

    void validateVertDistance(ExDNeighbour neighbour){
        ExDNode node1 = neighbour.reverseNeighbour.neighbourNode;
        ExDNode node2 = neighbour.neighbourNode;
        double vertDistance = PointModelUtil.verticalDistance(node1, node2);
        if (vertDistance != neighbour.vertDistance) {
            neighbour.vertDistance = (float) vertDistance;
            neighbour.reverseNeighbour.vertDistance = (float) -vertDistance;
        }
    }

    ExDNeighbour oppositeNeighbour(ExDNode node, ExDNeighbour givenNeighbour) {
        if (node.neighbourCnt != 2) return null;
        if (node.neighbour == givenNeighbour) return node.neighbour.nextNeighbour;
        if (node.neighbour.nextNeighbour == givenNeighbour) return node.neighbour;
        return null;
    }

    public short oppositeNeighbour(short nodeIdx, short givenNeighbourIdx){
        short firstNeighbour = getNdNeighbourIdx(nodeIdx);
        if (firstNeighbour == -1) return -1; // no neighbour
        short secondNeighbour = getNbNextNeighbourIdx(firstNeighbour);
        if (secondNeighbour == -1) return -1; // found just one neighbour
        short thirdNeighbour = getNbNextNeighbourIdx(secondNeighbour);
        if (thirdNeighbour != -1) return -1; // found third neighbour
        if (firstNeighbour == givenNeighbourIdx){
            return secondNeighbour;
        }
        if (secondNeighbour == givenNeighbourIdx){
            return firstNeighbour;
        }
        return -1; // should not happen (given neighbourNode is not neighbour to node
    }

    public ArrayList<MultiPointModel> getRawWays() {
        return rawWays;
    }

//    String dump(){
//        StringBuilder sb = new StringBuilder();
//        sb.append(this).append("\n");
//        sb.append("  BORDER_NODE_SOUTH: ").append(neighbourTiles[Border.BORDER_NODE_SOUTH]).append("\n");
//        sb.append("  BORDER_NODE_EAST: ").append(neighbourTiles[Border.BORDER_NODE_EAST]).append("\n");
//        sb.append("  BORDER_NODE_NORTH: ").append(neighbourTiles[Border.BORDER_NODE_NORTH]).append("\n");
//        sb.append("  BORDER_NODE_WEST: ").append(neighbourTiles[Border.BORDER_NODE_WEST]).append("\n");
//        for (short i=0; i<maxNodeCnt; i++){
//            sb.append(nodeToString(i)).append("\n");
//            if (nodeRefs[0][i] != null) sb.append("  ").append(nodeRefs[0][i].toString()).append("\n");
//            if (nodeRefs[1][i] != null) sb.append("  ").append(nodeRefs[1][i].toString()).append("\n");
//        }
//        for (short i=0; i<maxNeighbourCnt; i++){
//            sb.append(neighbourToString(i)).append("\n");
//        }
//        return sb.toString();
//    }

}
