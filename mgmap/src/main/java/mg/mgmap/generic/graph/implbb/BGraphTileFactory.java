package mg.mgmap.generic.graph.implbb;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.graph.impl.GGraph;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GNode;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BGraphTileFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

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
        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                WayAttributs wayAttributs = new WayAttributs(way);
                bGraphTile.addLatLongs( wayAttributs, way.latLongs[0]);

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

}
