package mg.mgmap.generic.graph.implbb;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;


import mg.mgmap.generic.util.basic.MGLog;

import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_EAST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_SOUTH;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_WEST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_NORTH;


public class BGraphMulti {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final BGraphTileFactory bGraphTileFactory;
    private final ArrayList<BGraphTile> bGraphTiles = new ArrayList<>();

    public BGraphMulti(BGraphTileFactory bGraphTileFactory, ArrayList<BGraphTile> bGraphTiles){
        this.bGraphTileFactory = bGraphTileFactory;
        for (BGraphTile bGraphTile : bGraphTiles){
            use(bGraphTile);
        }
    }

    BGraphTile getTile(short idxInMulti){
        return bGraphTiles.get(idxInMulti);
    }
    int getNumTiles(){
        return bGraphTiles.size();
    }

//    public int getTileCount(){
//        return bGraphTiles.size();
//    }

// TODO Verify, if we need this method at all
//    /**
//     * Redefines getNodes() implementation of GGraph, which simply returns the ArrayList of its nodes.
//     * Here we get a new ArrayList Object with all nodes from each included GGraphTile
//     * @return all node ot the multi graph
//     */
//
//    public ArrayList<GNode> getGNodes() {
//        ArrayList<GNode> nodes = new ArrayList<>( );
//        for (BGraphTile bGraphTile : bGraphTileFactory.getCached()){
//            if (bGraphTile.used){
//                nodes.addAll( bGraphTile.getGNodes() );
//            }
//        }
//        return nodes;
//    }

    // return true, if graph changed
    boolean preNodeRelax(BGraphTile bGraphTile, short node){
        boolean changed = false;
        byte nodeBorder = bGraphTile.nodes.getBorder(node);
        if (nodeBorder != 0) {
            changed |= checkBGraphTileNeighbour(bGraphTile, node, nodeBorder, BORDER_WEST);
            changed |= checkBGraphTileNeighbour(bGraphTile, node, nodeBorder, BORDER_NORTH);
            changed |= checkBGraphTileNeighbour(bGraphTile, node, nodeBorder, BORDER_EAST);
            changed |= checkBGraphTileNeighbour(bGraphTile, node, nodeBorder, BORDER_SOUTH);
        }
        return changed;
    }

    // Returns true, if graph is extended
    private boolean checkBGraphTileNeighbour(BGraphTile bGraphTile, short node, byte nodeBorder, byte border){
        boolean bRes = false;
        if ( (nodeBorder & border) != 0 ) {
            int tileXn = bGraphTile.getTileX() + BGraphTile.deltaX(border);
            int tileYn = bGraphTile.getTileY() + BGraphTile.deltaY(border);
            BGraphTile bGraphTileNeighbour = bGraphTileFactory.getBGraphTile(tileXn, tileYn, false);

            if (bGraphTileNeighbour == null){
                bGraphTileNeighbour = bGraphTileFactory.getBGraphTile(tileXn, tileYn, true);
                mgLog.d(String.format(Locale.ENGLISH, "tileX=%d tileY=%d use=%b border=%d tileXn=%d tileYn=%d use=%b",bGraphTile.getTileX(),bGraphTile.getTileY(),bGraphTile.idxInMulti,border,tileXn,tileYn,bGraphTileNeighbour.idxInMulti));
                bRes = true;
            }
            use(bGraphTileNeighbour);
        }

        return bRes;
    }

    private void use(BGraphTile bGraphTile){
        if (bGraphTile.idxInMulti < 0){
            bGraphTile.createNodeRefs();
            bGraphTile.idxInMulti = (short)bGraphTiles.size();
            bGraphTiles.add(bGraphTile);
            mgLog.d("use tileX="+bGraphTile.getTileX()+" tileY="+bGraphTile.getTileY()+" size: "+bGraphTiles.size());
        }
    }

    public void finalizeUsage(){
        mgLog.d("finalizeUsage A cntUsed="+bGraphTiles.size());
        for (BGraphTile bGraphTile : bGraphTileFactory.getCached()){
            bGraphTile.idxInMulti = -1;
        }
        bGraphTiles.clear();
        mgLog.d("finalizeUsage E cntUsed="+bGraphTiles.size());
    }

//    public int cntUsed(){
//        return (int)bGraphTileFactory.getCached().stream().filter(f->f.used).count();
//    }

}
