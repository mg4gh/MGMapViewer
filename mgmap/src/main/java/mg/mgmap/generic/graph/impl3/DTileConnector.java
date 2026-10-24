package mg.mgmap.generic.graph.impl3;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;


import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class DTileConnector {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    /** symmetric connect dGraphTile1 and dGraphTile2  */
    static void connect(DGraphTile dGraphTile1, DGraphTile dGraphTile2, boolean horizontal){ // horizontal true: dGraphTile1 is left, dGraphTile2 is right - false: dGraphTile1 is above, dGraphTile2 is below
        if ((dGraphTile1 == null) || (dGraphTile2 == null)) return; // cannot connect yet
        assert(horizontal?(dGraphTile1.tile.tileX+1 == dGraphTile2.tile.tileX):(dGraphTile1.tile.tileY+1 == dGraphTile2.tile.tileY));
        if ((dGraphTile1.neighbourTiles[horizontal? Border.BORDER_NODE_EAST: Border.BORDER_NODE_SOUTH] == dGraphTile2) &&
                (dGraphTile2.neighbourTiles[horizontal? Border.BORDER_NODE_WEST: Border.BORDER_NODE_NORTH] == dGraphTile1)){ // tiles already connected
            return;
        }
        if ((dGraphTile1.neighbourTiles[horizontal? Border.BORDER_NODE_EAST: Border.BORDER_NODE_SOUTH] != null) ||
                (dGraphTile2.neighbourTiles[horizontal? Border.BORDER_NODE_WEST: Border.BORDER_NODE_NORTH] != null)){ // inconsistency detected!!!!
            mgLog.e("connectTiles failed"+dGraphTile1.tile.tileX+","+dGraphTile1.tile.tileY+" "+dGraphTile2.tile.tileX+","+dGraphTile2.tile.tileY+" "+horizontal);
            mgLog.e("found="+dGraphTile1.neighbourTiles[horizontal? Border.BORDER_NODE_EAST: Border.BORDER_NODE_SOUTH]+" expected="+dGraphTile2);
            mgLog.e("found="+dGraphTile2.neighbourTiles[horizontal? Border.BORDER_NODE_WEST: Border.BORDER_NODE_NORTH]+" expected="+dGraphTile1);
            throw new IllegalArgumentException("inconsistent neighbour tiles - check logfile for more details.");
        }
//        double connectAt = (horizontal)?dGraphTile1.bBox.maxLongitude:dGraphTile1.bBox.minLatitude;

        ArrayList<Short> borderNodeIdxs1 = new ArrayList<>();
        short connectAt = horizontal? (short) LaLo.d2md(dGraphTile1.bBox.maxLongitude-dGraphTile1.bBox.minLongitude) : 0;
        for (short idx=0; idx < dGraphTile1.initialNodeCnt; idx++){
            short dLx = dGraphTile1.nodesBuffer.getShort(idx*DGraphTile.NODE_SIZE + (horizontal?DGraphTile.ND_LO_OFFSET :DGraphTile.ND_LA_OFFSET));
            if (dLx == connectAt){
                borderNodeIdxs1.add(idx);
            }
        }
        ArrayList<Short> borderNodeIdxs2 = new ArrayList<>();
        connectAt = horizontal? 0 : (short) LaLo.d2md(dGraphTile1.bBox.maxLatitude-dGraphTile1.bBox.minLatitude);
        for (short idx=0; idx < dGraphTile2.initialNodeCnt; idx++){
            short dLx = dGraphTile2.nodesBuffer.getShort(idx*DGraphTile.NODE_SIZE + (horizontal?DGraphTile.ND_LO_OFFSET :DGraphTile.ND_LA_OFFSET));
            if (dLx == connectAt){
                borderNodeIdxs2.add(idx);
            }
        }
        final ArrayList<Short> remainingNodeIdxs1 = new ArrayList<>(borderNodeIdxs1);
        final ArrayList<Short> remainingNodeIdxs2 = new ArrayList<>(borderNodeIdxs2);
        double threshold = (horizontal)? PointModelUtil.latitudeDistance(Config.CONNECT_THRESHOLD_METER):PointModelUtil.longitudeDistance(Config.CONNECT_THRESHOLD_METER, dGraphTile1.bBox.minLatitude);
        for (short idx1 : borderNodeIdxs1){
            for (short idx2 : borderNodeIdxs2){

                int offset = horizontal?DGraphTile.ND_LO_OFFSET :DGraphTile.ND_LA_OFFSET;
                if (dGraphTile1.nodesBuffer.getShort(idx1*DGraphTile.NODE_SIZE + offset) - dGraphTile2.nodesBuffer.getShort(idx2*DGraphTile.NODE_SIZE + offset) <= threshold) {
                    bidirectionalConnect(dGraphTile1, idx1, dGraphTile2, idx2, horizontal);
                    remainingNodeIdxs1.remove(idx1);
                    remainingNodeIdxs2.remove(idx2);
                }
            }
        }

        if ((!remainingNodeIdxs1.isEmpty()) && ((!remainingNodeIdxs2.isEmpty()))){
            WriteablePointModel approachPoint = new WriteablePointModelImpl();
            ArrayList<Short> stillRemainingNodes1 = new ArrayList<>(remainingNodeIdxs1);
            ArrayList<Short> stillRemainingNodes2 = new ArrayList<>(remainingNodeIdxs2);
            mgLog.v(remainingNodeIdxs1::toString);
            mgLog.v(remainingNodeIdxs2::toString);
            for (short nodeIdx1 : remainingNodeIdxs1){
                dGraphTile1.nodesBuffer.position(nodeIdx1 * DGraphTile.NODE_SIZE);
                double node1Lat = dGraphTile1.bBox.minLatitude + LaLo.md2d(dGraphTile1.nodesBuffer.getShort());
                double node1Lon = dGraphTile1.bBox.minLongitude + LaLo.md2d(dGraphTile1.nodesBuffer.getShort());
                short neighbourIdx1 = dGraphTile1.nodesBuffer.getShort(nodeIdx1 * DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET);
                dGraphTile1.neighboursBuffer.position(neighbourIdx1 * DGraphTile.NEIGHBOUR_SIZE + DGraphTile.NB_NEIGHBOUR_NODE_OFFSET);
                short neighbourNodeIdx1 = dGraphTile1.neighboursBuffer.getShort();
                short nextNeighbourIdx1 = dGraphTile1.neighboursBuffer.getShort();

                if (nextNeighbourIdx1 == -1){ // ok, just countNeighbours==1
                    dGraphTile1.nodesBuffer.position(neighbourNodeIdx1 * DGraphTile.NODE_SIZE);
                    double neighbourNode1Lat = dGraphTile1.bBox.minLatitude + LaLo.md2d(dGraphTile1.nodesBuffer.getShort());
                    double neighbourNode1Lon = dGraphTile1.bBox.minLongitude + LaLo.md2d(dGraphTile1.nodesBuffer.getShort());

                    for (short nodeIdx2 : remainingNodeIdxs2) {
                        dGraphTile2.nodesBuffer.position(nodeIdx2 * DGraphTile.NODE_SIZE);
                        double node2Lat = dGraphTile2.bBox.minLatitude + LaLo.md2d(dGraphTile2.nodesBuffer.getShort());
                        double node2Lon = dGraphTile2.bBox.minLongitude + LaLo.md2d(dGraphTile2.nodesBuffer.getShort());
                        short neighbourIdx2 = dGraphTile2.nodesBuffer.getShort(nodeIdx2 * DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET);
                        dGraphTile2.neighboursBuffer.position(neighbourIdx2 * DGraphTile.NEIGHBOUR_SIZE + DGraphTile.NB_NEIGHBOUR_NODE_OFFSET);
                        short neighbourNodeIdx2 = dGraphTile2.neighboursBuffer.getShort();
                        short nextNeighbourIdx2 = dGraphTile2.neighboursBuffer.getShort();

                        if (nextNeighbourIdx2 == -1){ // ok, also just countNeighbours==1
                            dGraphTile2.nodesBuffer.position(neighbourNodeIdx2 * DGraphTile.NODE_SIZE);
                            double neighbourNode2Lat = dGraphTile2.bBox.minLatitude + LaLo.md2d(dGraphTile2.nodesBuffer.getShort());
                            double neighbourNode2Lon = dGraphTile2.bBox.minLongitude + LaLo.md2d(dGraphTile2.nodesBuffer.getShort());

                            double distance = PointModelUtil.distance(node1Lat, node1Lon, node2Lat, node2Lon);
                            if (distance < Config.CONNECT_THRESHOLD_METER*20){
                                if (!PointModelUtil.findApproach(node1Lat, node1Lon, neighbourNode1Lat, neighbourNode1Lon, 0, 0, neighbourNode2Lat, neighbourNode2Lon, 0, 0, approachPoint, 0)) continue;
                                if (PointModelUtil.distance(approachPoint.getLat(), approachPoint.getLon(), node1Lat, node1Lon) > Config.CONNECT_THRESHOLD_METER) continue;
                                if (!PointModelUtil.findApproach(node2Lat, node2Lon, neighbourNode1Lat, neighbourNode1Lon, 0, 0, neighbourNode2Lat, neighbourNode2Lon, 0, 0, approachPoint, 0)) continue;
                                if (PointModelUtil.distance(approachPoint.getLat(), approachPoint.getLon(), node2Lat, node2Lon) > Config.CONNECT_THRESHOLD_METER) continue;
                                mgLog.d(()->String.format(Locale.ENGLISH,"OK, connect: node1=%.6f,%.6f node2=%.6f,%.6f", node1Lat, node1Lon, node2Lat, node2Lon));
                                bidirectionalConnect(dGraphTile1, nodeIdx1, dGraphTile2, nodeIdx2, horizontal);
                                stillRemainingNodes1.remove(nodeIdx1);
                                stillRemainingNodes2.remove(nodeIdx2);
                            }
                        }
                    }
                }
            }
            remainingNodeIdxs1.clear();
            remainingNodeIdxs1.addAll(stillRemainingNodes1);
            remainingNodeIdxs2.clear();
            remainingNodeIdxs2.addAll(stillRemainingNodes2);
        }
        if ((!remainingNodeIdxs1.isEmpty()) || ((!remainingNodeIdxs2.isEmpty()))){
            mgLog.d(()->"remainings1 " + remainingNodeIdxs1);
            mgLog.d(()->"remainings2 " + remainingNodeIdxs2);
        }
        dGraphTile1.neighbourTiles[horizontal? Border.BORDER_NODE_EAST: Border.BORDER_NODE_SOUTH] = dGraphTile2;
        dGraphTile2.neighbourTiles[horizontal? Border.BORDER_NODE_WEST: Border.BORDER_NODE_NORTH] = dGraphTile1;
    }


    /**
     * Asymmetric operation, remove all references to dGraphTile and its nodes (so garbage collector can remove it)
     * @param dGraphTile tile that should not longer be connected
     * @param border border of tile where neighbourTile should not longer reference to the tile or nodes of the tile
     */
    static void disconnect(DGraphTile dGraphTile, byte border){
        DGraphTile neighbourTile = dGraphTile.neighbourTiles[border];
        if (neighbourTile == null) return; // nothing to do
        byte neighboursBorder = Border.oppositeBorder(border);
        neighbourTile.removeNeighbours(neighboursBorder);
        neighbourTile.neighbourTiles[neighboursBorder] = null;
    }



    // Use this only for connect of different tiles - tile1 left/up , tile2 right/down
    public static void bidirectionalConnect(DGraphTile tile1, short nodeIdx1, DGraphTile tile2, short nodeIdx2, boolean horizontal){
        short newNeighbourIdxTile1 = tile1.findFreeInterconnectNeighbour();
        short newNeighbourIdxTile2 = tile2.findFreeInterconnectNeighbour();

        byte flags = (byte)(DGraphTile.NB_FLAG_VALID | DGraphTile.NB_FLAG_PRIMARY_DIRECTION);
        short nextNeighbourIdx1 = tile1.nodesBuffer.getShort(nodeIdx1*DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET);
        tile1.addNeighbourAt(newNeighbourIdxTile1, nodeIdx2,nextNeighbourIdx1,newNeighbourIdxTile2,(short)-1, horizontal?Border.BORDER_NODE_EAST:Border.BORDER_NODE_SOUTH, flags);
        tile1.nodesBuffer.putShort(nodeIdx1*DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET, newNeighbourIdxTile1);

        short nextNeighbourIdx2 = tile2.nodesBuffer.getShort(nodeIdx2*DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET);
        tile1.addNeighbourAt(newNeighbourIdxTile2, nodeIdx1,nextNeighbourIdx2,newNeighbourIdxTile1,(short)-1, horizontal?Border.BORDER_NODE_WEST:Border.BORDER_NODE_NORTH, flags);
        tile2.nodesBuffer.putShort(nodeIdx2*DGraphTile.NODE_SIZE + DGraphTile.ND_NEIGHBOUR_OFFSET, newNeighbourIdxTile2);
    }


}
