package mg.mgmap.generic.graph.implbb;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.impl.GNeighbour;
import mg.mgmap.generic.graph.impl.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BGraphTest {

    private MGLog mgLog;

    @Test
    public void _01_tile_nodes() {
        PointModelUtil.init(32);
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
//        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
//        BBox bBox = new BBox().extend(49.0, 8.0);
        BBox bBox = new BBox().extend(49.0, 8.0).extend(49.3, 8.3);
//        BBox bBox = new BBox().extend(49.699614769647766, 8.0914306640625);
//        BBox bBox = new BBox().extend(49.301260,8.089355);
//        BBox bBox = new BBox().extend(49.282140,8.001331).extend(50);

        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));
        ArrayList<GGraphTile> gGraphTiles = gGraphTileFactory.getGGraphTileList(bBox);
        BGraphTileFactory bGraphTileFactory = new BGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(""), new Pref<>(false));
        ArrayList<BGraphTile> bGraphTiles = bGraphTileFactory.getBGraphTileList(bBox);

        assert (gGraphTiles.size() == bGraphTiles.size()):"g="+gGraphTiles.size()+" b="+bGraphTiles.size();
        for (int iTile=0; iTile < gGraphTiles.size(); iTile++){
            GGraphTile gGraphTile = gGraphTiles.get(iTile);
            BGraphTile bGraphTile = bGraphTiles.get(iTile);

            ArrayList<GNode> gNodes = new ArrayList<>( gGraphTile.getGNodes() );
            int invalidPoints = 0;
            for (short nodeIdx = 0; nodeIdx<bGraphTile.nodes.nodesUsed; nodeIdx++){
                mgLog.d("XXXX iTile="+iTile+" nodeIdx="+nodeIdx);
                if (isValid(bGraphTile, nodeIdx)){
                    GNode gNode = identifyGNode(gNodes, bGraphTile, nodeIdx);
                    deepCompare(gNode, bGraphTile, nodeIdx, true);
                } else {
                    invalidPoints ++;
                }
            }
            Assert.assertEquals (gNodes.size() , bGraphTile.nodes.nodesUsed - invalidPoints);

        }




    }

    TreeSet<GNode> neighbours(GNode gNode, boolean ignoreNeighbourTile){
        TreeSet<GNode> res = new TreeSet<>();
        GNeighbour xNeighbour = gNode.getNeighbour();
        while (xNeighbour != null){
            mgLog.d("GN: "+xNeighbour.getNeighbourNode()+" "+xNeighbour.getNeighbourNode().tileIdx);
            GNode xNode = xNeighbour.getNeighbourNode();
            if (!ignoreNeighbourTile || (gNode.tileIdx == xNode.tileIdx)){
                res.add(xNeighbour.getNeighbourNode());
            }
            xNeighbour = xNeighbour.getNextNeighbour();
        }
        return res;
    }
    TreeSet<PointModelImpl> neighbours(BGraphTile bGraphTile, short nodeIdx, boolean ignoreNeighbourTile){
        TreeSet<PointModelImpl> res = new TreeSet<>();
        short xNeighbour = bGraphTile.nodes.getNeighbour(nodeIdx);
        while (xNeighbour != 0){
            short gnn = bGraphTile.neighbours.getNeighbourPoint(xNeighbour);
            PointModelImpl pmi = PointModelImpl.createFromLaLo( bGraphTile.nodes.getLatitude(gnn), bGraphTile.nodes.getLongitude(gnn) );
            mgLog.d("BN: "+pmi);
            if (!ignoreNeighbourTile || (bGraphTile.neighbours.getTileSelector(xNeighbour) == 0)){
                res.add(pmi);
            }
            xNeighbour = bGraphTile.neighbours.getNextNeighbour(xNeighbour);
        }
        return res;
    }


    boolean isValid( BGraphTile bGraphTile, short bNode){
        return !bGraphTile.nodes.isFlag(bNode,BNodes.FLAG_INVALID);
    }
    void deepCompare( GNode gNode, BGraphTile bGraphTile, short bNode, boolean ignoreNeighbourTile){
        int level = 0;
        compare(gNode, bGraphTile, bNode, level++);
        TreeSet<GNode> gNeighbours = neighbours(gNode, ignoreNeighbourTile);
        TreeSet<PointModelImpl> bNeighbours = neighbours(bGraphTile, bNode, ignoreNeighbourTile);
        assert (gNeighbours.size() == bNeighbours.size()):gNeighbours.size()+" "+ bNeighbours.size();
        while (!gNeighbours.isEmpty()){
            GNode first = gNeighbours.first();
            gNeighbours.remove(first);
            bNeighbours.remove(first);
        }
        assert(bNeighbours.isEmpty());

    }
    GNode identifyGNode(ArrayList<GNode> gNodes, BGraphTile bGraphTile, short bNode){
        int la = bGraphTile.nodes.getLatitude(bNode);
        int lo = bGraphTile.nodes.getLongitude(bNode);
        int gIdx = gNodes.indexOf( PointModelImpl.createFromLaLo(la, lo));
        return gNodes.get(gIdx);
    }
    void compare( GNode gNode, BGraphTile bGraphTile, short bNode, int level){
        int la = bGraphTile.nodes.getLatitude(bNode);
        int lo = bGraphTile.nodes.getLongitude(bNode);
        float ele = bGraphTile.nodes.getEle(bNode);

        String s = "";
        for (int i=0; i<level; i++) s += "  ";
        mgLog.d(String.format(Locale.ENGLISH, "XXXX %s %s %d  %d %d+%d", s, gNode, gNode.tileIdx, bNode, la, lo));

        assert (gNode.getLat() == LaLo.md2d(la)):"level="+level;
        assert (gNode.getLon() == LaLo.md2d(lo)):"level="+level;
        assert (gNode.getEle() == ele):"level="+level;
    }
}
