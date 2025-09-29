package mg.mgmap.generic.graph.impl2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

public class GTileConnector {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m


    /** symmetric connect gGraphTile1 and gGraphTile2  */
    static void connect(GGraphTile gGraphTile1, GGraphTile gGraphTile2, boolean horizontal){ // horizontal true: gGraphTile1 is left, gGraphTile2 is right - false: gGraphTile1 is above, gGraphTile2 is below
        if ((gGraphTile1 == null) || (gGraphTile2 == null)) return; // cannot connect yet
        if ((gGraphTile1.neighbourTiles[horizontal? GNode.BORDER_NODE_EAST: GNode.BORDER_NODE_SOUTH] == gGraphTile2) &&
                (gGraphTile2.neighbourTiles[horizontal? GNode.BORDER_NODE_WEST: GNode.BORDER_NODE_NORTH] == gGraphTile1)){ // tiles already connected
            return;
        }
        if ((gGraphTile1.neighbourTiles[horizontal? GNode.BORDER_NODE_EAST: GNode.BORDER_NODE_SOUTH] != null) ||
                (gGraphTile2.neighbourTiles[horizontal? GNode.BORDER_NODE_WEST: GNode.BORDER_NODE_NORTH] != null)){ // inconsistency detected!!!!
            mgLog.e("connectTiles failed"+gGraphTile1.getTileX()+","+gGraphTile1.getTileY()+" "+gGraphTile2.getTileX()+","+gGraphTile2.getTileY()+" "+horizontal);
            mgLog.e("found="+gGraphTile1.neighbourTiles[horizontal? GNode.BORDER_NODE_EAST: GNode.BORDER_NODE_SOUTH]+" expected="+gGraphTile2);
            mgLog.e("found="+gGraphTile2.neighbourTiles[horizontal? GNode.BORDER_NODE_WEST: GNode.BORDER_NODE_NORTH]+" expected="+gGraphTile1);
            throw new IllegalArgumentException("inconsistent neighbour tiles - check logfile for more details.");
        }
        double connectAt = (horizontal)?gGraphTile1.bBox.maxLongitude:gGraphTile1.bBox.minLatitude;
        if (horizontal? (connectAt!=gGraphTile2.bBox.minLongitude):(connectAt!=gGraphTile2.bBox.maxLatitude)){
            throw new IllegalArgumentException("cannot connectHorizontal Tiles with BB " + gGraphTile1.bBox +" and "+gGraphTile2.bBox);
        }
        ArrayList<GNode> borderNodes1 = new ArrayList<>();
        for (GNode node : gGraphTile1.getGNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes1.add(node);
            }
        }
        ArrayList<GNode> borderNodes2 = new ArrayList<>();
        for (GNode node : gGraphTile2.getGNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes2.add(node);
            }
        }
        final ArrayList<GNode> remainingNodes1 = new ArrayList<>(borderNodes1);
        final ArrayList<GNode> remainingNodes2 = new ArrayList<>(borderNodes2);
        double threshold = (horizontal)? PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER):PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, connectAt);
        for (GNode node1 : borderNodes1){
            for (GNode node2 : borderNodes2){
                if ( (horizontal?Math.abs( node1.getLat() - node2.getLat() ):Math.abs( node1.getLon() - node2.getLon() )) <= threshold){ //distance less than 0.5m -> connect nodes
                    gGraphTile1.bidirectionalConnect(node1, node2, null); // no need to add this to gGraphTile2 too, since nodes are linked
                    remainingNodes1.remove(node1);
                    remainingNodes2.remove(node2);
                }
            }
        }
        if ((!remainingNodes1.isEmpty()) && ((!remainingNodes2.isEmpty()))){
            ArrayList<GNode> stillRemainingNodes1 = new ArrayList<>(remainingNodes1);
            ArrayList<GNode> stillRemainingNodes2 = new ArrayList<>(remainingNodes2);
            mgLog.v(remainingNodes1::toString);
            mgLog.v(remainingNodes2::toString);
            for (GNode node1 : remainingNodes1){
                for (GNode node2 : remainingNodes2){
                    if ((gGraphTile1.countNeighbours(node1) == 1) && (gGraphTile2.countNeighbours(node2) == 1) && (PointModelUtil.distance(node1,node2)<CONNECT_THRESHOLD_METER*20)){
                        GNeighbour neighbour1 = gGraphTile1.getNextNeighbour(node1, null);
                        GNeighbour neighbour2 = gGraphTile2.getNextNeighbour(node2, null);
                        PointModel node1Neighbour = (neighbour1.cntIntermediates()==0)?neighbour1.getNeighbourNode():new GIntermediateNode(node1, neighbour1, 0);
                        PointModel node2Neighbour = (neighbour2.cntIntermediates()==0)?neighbour2.getNeighbourNode():new GIntermediateNode(node2, neighbour2, 0);
                        WriteablePointModel approachPoint = new WriteablePointModelImpl();
                        if (!PointModelUtil.findApproach(node1,node1Neighbour,node2Neighbour,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node1) > CONNECT_THRESHOLD_METER) continue;
                        if (!PointModelUtil.findApproach(node2,node1Neighbour,node2Neighbour,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node2) > CONNECT_THRESHOLD_METER) continue;
                        mgLog.d(()->"OK, connect: node1 " + node1 + " node1neighbour " + node1Neighbour + " node2 " + node2 + " node2neighbour " + node2Neighbour);
                        gGraphTile1.bidirectionalConnect(node1, node2, null); // no need to add this to gGraphTile2 too, since nodes are linked
                        stillRemainingNodes1.remove(node1);
                        stillRemainingNodes2.remove(node2);
                    }
                }
            }
            remainingNodes1.clear();
            remainingNodes1.addAll(stillRemainingNodes1);
            remainingNodes2.clear();
            remainingNodes2.addAll(stillRemainingNodes2);
        }
        if ((!remainingNodes1.isEmpty()) || ((!remainingNodes2.isEmpty()))){
            mgLog.d(()->"remainings1 " + remainingNodes1);
            mgLog.d(()->"remainings2 " + remainingNodes2);
        }
        gGraphTile1.neighbourTiles[horizontal? GNode.BORDER_NODE_EAST: GNode.BORDER_NODE_SOUTH] = gGraphTile2;
        gGraphTile2.neighbourTiles[horizontal? GNode.BORDER_NODE_WEST: GNode.BORDER_NODE_NORTH] = gGraphTile1;
    }

    /**
     * Asymmetric operation, remove all references to gGraphTile and its nodes (so garbage collector can remove it)
     * @param gGraphTile tile that should not longer be connected
     * @param border border of tile where neighbourTile should not longer reference to the tile or nodes of the tile
     */
   static void disconnect(GGraphTile gGraphTile, byte border){
        GGraphTile neighbourTile = gGraphTile.neighbourTiles[border];
        if (neighbourTile == null) return; // nothing to do
        byte neighboursBorder = GNode.oppositeBorder(border);
        for (GNode node : neighbourTile.getGNodes()){
            if ((node.borderNode & neighboursBorder) != 0){
                neighbourTile.removeNeighbourTo(node, gGraphTile.tileIdx);
            }
        }
        assert (neighbourTile.neighbourTiles[neighboursBorder].tileIdx == gGraphTile.tileIdx)
                :"neighbourTiles[neighboursBorder].tileIdx"+neighbourTile.neighbourTiles[neighboursBorder].tileIdx+" tileIdx="+gGraphTile.tileIdx;
        neighbourTile.neighbourTiles[neighboursBorder] = null;
    }


}
