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
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.impl.GNeighbour;
import mg.mgmap.generic.graph.impl.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
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
//        PointModelImpl pm1 = new PointModelImpl(49.402665, 8.686337);
//        PointModelImpl pm2 = new PointModelImpl(49.404388, 8.685524);
//        BBox bBox = new BBox().extend(pm1).extend(pm2);
//        BBox bBox = new BBox().extend(49.699614769647766, 8.0914306640625);
//        BBox bBox = new BBox().extend(49.301260,8.089355);
//        BBox bBox = new BBox().extend(49.282140,8.001331).extend(50);

        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));
        long ts1g = System.currentTimeMillis();
        ArrayList<GGraphTile> gGraphTiles = gGraphTileFactory.getGGraphTileList(bBox);
        long ts2g = System.currentTimeMillis();
        BGraphTileFactory bGraphTileFactory = new BGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(""), new Pref<>(false));
        long ts1b = System.currentTimeMillis();
        ArrayList<BGraphTile> bGraphTiles = bGraphTileFactory.getBGraphTileList(bBox);
        long ts2b = System.currentTimeMillis();

        mgLog.i("XXXX g Duration="+(ts2g-ts1g)+" b Duration="+(ts2b-ts1b));



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
                    deepCompare(gNode, bGraphTile, nodeIdx, false);
                } else {
                    invalidPoints ++;
                }
            }
            Assert.assertEquals (gNodes.size() , bGraphTile.nodes.nodesUsed - invalidPoints);

        }




    }

    ArrayList<GNode> neighbours(GNode gNode, boolean ignoreNeighbourTile){
        ArrayList<GNode> res = new ArrayList<>();
        GNeighbour xNeighbour = gNode.getNeighbour();
        while (xNeighbour != null){
            mgLog.d("GN: "+xNeighbour.getNeighbourNode()+" "+xNeighbour.getNeighbourNode().tileIdx);
            GNode xNode = xNeighbour.getNeighbourNode();
            if (gNode.tileIdx != xNode.tileIdx){
                mgLog.d("XXX different Tile");
            }
            if (!ignoreNeighbourTile || (gNode.tileIdx == xNode.tileIdx)){
                res.add(xNeighbour.getNeighbourNode());
            }
            xNeighbour = xNeighbour.getNextNeighbour();
        }
        return res;
    }
    ArrayList<PointModelImpl> neighbours(BGraphTile bGraphTile, short nodeIdx, boolean ignoreNeighbourTile){
        ArrayList<PointModelImpl> res = new ArrayList<>();
        short xNeighbour = bGraphTile.nodes.getNeighbour(nodeIdx);
        while (xNeighbour != 0){
            byte tileSelector = bGraphTile.neighbours.getTileSelector(xNeighbour);
            BGraphTile neighbourTile = bGraphTile;
            if (tileSelector != 0){
                neighbourTile = bGraphTile.neighbourTiles[tileSelector];
            }
            short neighbourNode = bGraphTile.neighbours.getNeighbourPoint(xNeighbour);
            PointModelImpl pmi = PointModelImpl.createFromLaLo( neighbourTile.nodes.getLatitude(neighbourNode), neighbourTile.nodes.getLongitude(neighbourNode) );
            mgLog.d("BN: "+pmi);
            if (!ignoreNeighbourTile || tileSelector == 0){
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
        ArrayList<GNode> gNeighbours = neighbours(gNode, ignoreNeighbourTile);
        ArrayList<PointModelImpl> bNeighbours = neighbours(bGraphTile, bNode, ignoreNeighbourTile);
        if (gNeighbours.size() != bNeighbours.size()){
            mgLog.d( );
        };
        assert (gNeighbours.size() == bNeighbours.size()):gNeighbours.size()+" "+ bNeighbours.size();
        while (!gNeighbours.isEmpty()){
            GNode firstG = gNeighbours.get(0);
            PointModel firstB = bNeighbours.get(0);
            Assert.assertEquals(firstG, firstB);
            gNeighbours.remove(firstG);
            bNeighbours.remove(firstB);
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
