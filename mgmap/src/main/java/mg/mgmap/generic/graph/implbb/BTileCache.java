package mg.mgmap.generic.graph.implbb;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.generic.util.basic.MGLog;

import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_EAST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_SOUTH;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_WEST;
import static mg.mgmap.generic.graph.implbb.BGraphTile.BORDER_NORTH;


public class BTileCache {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final int limit;
    public BTileCache(int limit){
        this.limit = limit;
    }

    final TreeMap<Integer, BGraphTile> tileMap = new TreeMap<>();
    final TreeMap<Long, Integer> accessMap = new TreeMap<>();

    synchronized void put (int tileX, int tileY, BGraphTile tile){
        int tileIdx = BGraphTileFactory.getKey(tileX,tileY);
        BGraphTile cacheTile = tileMap.put(tileIdx, tile);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
        }
        BTileConnector.connect(get( tileX-1,tileY), tile, true);
        BTileConnector.connect(tile, get( tileX+1,tileY), true);
        BTileConnector.connect(get( tileX,tileY-1), tile, false);
        BTileConnector.connect(tile, get( tileX,tileY+1), false);
        long now = System.nanoTime();
        tile.accessTime = now;
        accessMap.put(now, tileIdx);
        service2();
    }

    synchronized BGraphTile get(int tileX, int tileY){
        int tileIdx = BGraphTileFactory.getKey(tileX,tileY);
        BGraphTile cacheTile = tileMap.get(tileIdx);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
            long now = System.nanoTime();
            cacheTile.accessTime = now;
            if (cacheTile.idxInMulti < 0){
                accessMap.put(now, tileIdx); // maintain accessMap only for tiles outside of current route calculation
            }
        }
        return cacheTile;
    }

    synchronized ArrayList<BGraphTile> getAll(){
        return new ArrayList<>(tileMap.values());
    }

    synchronized void clear(){
        tileMap.clear();
        accessMap.clear();
    }

    synchronized int size(){
        return tileMap.size();
    }

    synchronized void service(){ // full service => rebuild accessMap based on tileMap
        accessMap.clear();
        for (BGraphTile tile : tileMap.values()){
            accessMap.put(tile.accessTime, tile.tileIdx);
        }
        service2();
    }

    private void service2(){
        int initialSize = tileMap.size();
        while (tileMap.size() > limit){
            Map.Entry<Long, Integer> entry = accessMap.firstEntry();
            if ((entry != null) && (accessMap.size() >= 2)){ // accessMap.size() >= 2 ensures that the recently added tile isn't removed from cache
                BGraphTile tile = tileMap.get(entry.getValue());
                assert (tile != null);
                if (tile.idxInMulti >= 0) {
                    accessMap.remove(tile.accessTime); // remove tile from access map, since cleanup is not allowed as long as it is used
                } else {
                    accessMap.remove(entry.getKey());
                    tileMap.remove(entry.getValue());
                    BTileConnector.disconnect(tile, BORDER_WEST);
                    BTileConnector.disconnect(tile, BORDER_NORTH);
                    BTileConnector.disconnect(tile, BORDER_EAST);
                    BTileConnector.disconnect(tile, BORDER_SOUTH);
//                    mgLog.d("remove from cache: tileX="+tile.getTileX()+" tileY="+tile.getTileY()+" access="+tile.accessTime);
                }
            } else {
                break;
            }
        }
        if (tileMap.size() != initialSize){
            mgLog.d("cleanup initialTileMapSize="+initialSize + " tileMapSize="+tileMap.size()+" accessMapSize="+accessMap.size());
        }
    }

}
