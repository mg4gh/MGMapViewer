package mg.mgmap.generic.graph.implbb;


import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_EAST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_SOUTH;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_WEST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_NORTH;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_NO;

public class BTileConnector {


    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m

    private static final int MAX_BORDER_NODE = 200;
    private static final short[] borderNodeIndexesInit = new short[MAX_BORDER_NODE];
    private static final short[] borderNodeIndexes1 = new short[MAX_BORDER_NODE];
    private static final short[] borderNodeIndexes2 = new short[MAX_BORDER_NODE];

    /** symmetric connect bGraphTile1 and bGraphTile2  */
    static void connect(BGraphTile bGraphTile1, BGraphTile bGraphTile2, boolean horizontal) { // horizontal true: bGraphTile1 is left, bGraphTile2 is right - false: bGraphTile1 is above, bGraphTile2 is below
        if ((bGraphTile1 == null) || (bGraphTile2 == null)) return; // cannot connect yet

        if ((bGraphTile1.neighbourTiles[horizontal? BORDER_EAST:BORDER_SOUTH] == bGraphTile2) &&
                (bGraphTile2.neighbourTiles[horizontal?BORDER_WEST:BORDER_NORTH] == bGraphTile1)){ // tiles already connected
            return;
        }
        if ((bGraphTile1.neighbourTiles[horizontal?BORDER_EAST:BORDER_SOUTH] != null) ||
                (bGraphTile2.neighbourTiles[horizontal?BORDER_WEST:BORDER_NORTH] != null)){ // inconsistency detected!!!!
            mgLog.e("connectTiles failed"+bGraphTile1.getTileX()+","+bGraphTile1.getTileY()+" "+bGraphTile2.getTileX()+","+bGraphTile2.getTileY()+" "+horizontal);
            mgLog.e("found="+bGraphTile1.neighbourTiles[horizontal?BORDER_EAST:BORDER_SOUTH]+" expected="+bGraphTile2);
            mgLog.e("found="+bGraphTile2.neighbourTiles[horizontal?BORDER_WEST:BORDER_NORTH]+" expected="+bGraphTile1);
            throw new IllegalArgumentException("inconsistent neighbour tiles - check logfile for more details.");
        }

        byte borderSelector1 = BORDER_NO;
        byte borderSelector2 = BORDER_NO;
        if (bGraphTile1.tile.tileX +1 == bGraphTile2.tileIdx){
            borderSelector1 = BORDER_EAST;
            borderSelector2 = BORDER_WEST;
        } else if (bGraphTile1.neighbourTiles[BORDER_WEST] == bGraphTile2){
            borderSelector1 = BORDER_WEST;
            borderSelector2 = BORDER_EAST;
        } else if (bGraphTile1.neighbourTiles[BORDER_SOUTH] == bGraphTile2){
            borderSelector1 = BORDER_SOUTH;
            borderSelector2 = BORDER_NORTH;
        } else if (bGraphTile1.neighbourTiles[BORDER_NORTH] == bGraphTile2){
            borderSelector1 = BORDER_NORTH;
            borderSelector2 = BORDER_SOUTH;
        }

        double connectAt = (horizontal)?bGraphTile1.bBox.maxLongitude:bGraphTile1.bBox.minLatitude;
        if (horizontal? (connectAt!=bGraphTile2.bBox.minLongitude):(connectAt!=bGraphTile2.bBox.maxLatitude)){
            throw new IllegalArgumentException("cannot connectHorizontal Tiles with BB " + bGraphTile1.bBox +" and "+bGraphTile2.bBox);
        }

        System.arraycopy(borderNodeIndexesInit, 0, borderNodeIndexes1, 0, MAX_BORDER_NODE);
        int numBorderNodes1 = 0;
        System.arraycopy(borderNodeIndexesInit, 0, borderNodeIndexes2, 0, MAX_BORDER_NODE);
        int numBorderNodes2 = 0;

        for (short i=0; i<bGraphTile1.nodes.nodesUsed; i++) {
            if ((horizontal) ? (bGraphTile1.nodes.getLongitude(i) == connectAt) : (bGraphTile1.nodes.getLatitude(i) == connectAt)) {
                borderNodeIndexes1[numBorderNodes1++] = i;
            }
        }
        for (short i=0; i<bGraphTile2.nodes.nodesUsed; i++) {
            if ((horizontal) ? (bGraphTile2.nodes.getLongitude(i) == connectAt) : (bGraphTile2.nodes.getLatitude(i) == connectAt)) {
                borderNodeIndexes2[numBorderNodes2++] = i;
            }
        }
        double threshold = (horizontal)? PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER):PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, connectAt);
        for (short idx1=0; idx1<numBorderNodes1; idx1++){
            for (short idx2=0; idx2<numBorderNodes2; idx2++){
                if ( (horizontal?Math.abs( bGraphTile1.nodes.getLatitude(idx1) - bGraphTile2.nodes.getLatitude(idx2) ):Math.abs( bGraphTile1.nodes.getLongitude(idx1) - bGraphTile2.nodes.getLongitude(idx2) )) <= threshold){ //distance less than 0.5m -> connect nodes
                    bGraphTile1.addSegment((short) -1, idx1, bGraphTile2, idx2, borderSelector1);
                    bGraphTile2.addSegment((short) -1, idx2, bGraphTile1, idx1, borderSelector2);
                    borderNodeIndexes1[idx1] = -1;
                    borderNodeIndexes2[idx2] = -1;
                }
            }
        }
        for (short idx1=0; idx1<numBorderNodes1; idx1++){
            if (borderNodeIndexes1[idx1] == -1) continue;

            for (short idx2=0; idx2<numBorderNodes2; idx2++) {
                if (borderNodeIndexes2[idx2] == -1) continue;

                if ((bGraphTile1.countNeighbours(idx1) == 1) && (bGraphTile2.countNeighbours(idx2) == 1)) {
                    double lat1 = LaLo.md2d(bGraphTile1.nodes.getLatitude(idx1));
                    double lon1 = LaLo.md2d(bGraphTile1.nodes.getLongitude(idx1));
                    double lat2 = LaLo.md2d(bGraphTile2.nodes.getLatitude(idx2));
                    double lon2 = LaLo.md2d(bGraphTile2.nodes.getLongitude(idx2));
                    if (PointModelUtil.distance(lat1, lon1, lat2, lon2) < CONNECT_THRESHOLD_METER * 20) {
                        BNode node1 = new BNode(bGraphTile1, idx1);
                        BNode node2 = new BNode(bGraphTile1, idx2);
                        BNode node1NeighbourNode = new BNode(bGraphTile1,  bGraphTile1.neighbours.getNeighbourPoint( bGraphTile1.neighbours.getNextNeighbour( bGraphTile1.nodes.getNeighbour(idx1) ) ));
                        BNode node2NeighbourNode = new BNode(bGraphTile2,  bGraphTile2.neighbours.getNeighbourPoint( bGraphTile2.neighbours.getNextNeighbour( bGraphTile2.nodes.getNeighbour(idx2) ) ));

                        WriteablePointModel approachPoint = new WriteablePointModelImpl();
                        if (!PointModelUtil.findApproach(node1,node1NeighbourNode,node2NeighbourNode,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node1) > CONNECT_THRESHOLD_METER) continue;
                        if (!PointModelUtil.findApproach(node2,node1NeighbourNode,node2NeighbourNode,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node2) > CONNECT_THRESHOLD_METER) continue;
                        mgLog.d(()->"OK, connect: node1 " + node1 + " node1NeighbourNode " + node1NeighbourNode + " node2 " + node2 + " node2NeighbourNode " + node2NeighbourNode);

                        bGraphTile1.addSegment((short) -1, idx1, bGraphTile2, idx2, borderSelector1);
                        bGraphTile2.addSegment((short) -1, idx2, bGraphTile1, idx1, borderSelector2);
                        borderNodeIndexes1[idx1] = -1;
                        borderNodeIndexes2[idx2] = -1;

                    }
                }
            }
        }
        StringBuilder sb1 = new StringBuilder("tile1.idx="+bGraphTile1.tileIdx);
        boolean remainings = false;
        for (short idx1=0; idx1<numBorderNodes1; idx1++) {
            if (borderNodeIndexes1[idx1] == -1) continue;
            sb1.append(",").append(idx1);
            remainings = true;
        }
        if (remainings) mgLog.d(()->"remainings1 " + sb1);
        StringBuilder sb2 = new StringBuilder("tile2.idx="+bGraphTile2.tileIdx);
        for (short idx2=0; idx2<numBorderNodes2; idx2++) {
            if (borderNodeIndexes2[idx2] == -1) continue;
            sb2.append(",").append(idx2);
            remainings = true;
        }
        if (remainings) mgLog.d(()->"remainings2 " + sb1);

        bGraphTile1.neighbourTiles[horizontal?BORDER_EAST:BORDER_SOUTH] = bGraphTile2;
        bGraphTile2.neighbourTiles[horizontal?BORDER_WEST:BORDER_NORTH] = bGraphTile1;
    }



    /**
     * Asymmetric operation, remove all references to gGraphTile and its nodes (so garbage collector can remove it)
     * @param bGraphTile tile that should not longer be connected
     * @param border border of tile where neighbourTile should not longer reference to the tile or nodes of the tile
     */
    static void disconnect(BGraphTile bGraphTile, byte border){
        BGraphTile neighbourTile = bGraphTile.neighbourTiles[border];
        if (neighbourTile == null) return; // nothing to do
        byte neighboursBorder = oppositeBorder(border);
        for (short node=0; node<neighbourTile.nodes.nodesUsed; node++){
            if ((bGraphTile.nodes.getBorder(node) & neighboursBorder) != 0) {
                neighbourTile.removeNeighbourToTile(node, neighboursBorder);
            }
        }
        assert (neighbourTile.neighbourTiles[neighboursBorder].tileIdx == bGraphTile.tileIdx)
                :"neighbourTiles[neighboursBorder].tileIdx"+neighbourTile.neighbourTiles[neighboursBorder].tileIdx+" tileIdx="+bGraphTile.tileIdx;
        neighbourTile.neighbourTiles[neighboursBorder] = null;
    }


    public static byte oppositeBorder(byte border){
        return switch (border) {
            case BORDER_WEST -> BORDER_EAST;
            case BORDER_EAST -> BORDER_WEST;
            case BORDER_NORTH -> BORDER_SOUTH;
            case BORDER_SOUTH -> BORDER_NORTH;
            default -> 0;
        };
    }


}
