package mg.mgmap.generic.graph.impl3;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.generic.util.basic.MGLog;

public class DTileCache {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final int limit;
    public DTileCache(int limit){
        this.limit = limit;
    }

    /** map tileIdx to GraphTile object */
    final TreeMap<Integer, DGraphTile> tileMap = new TreeMap<>();
    /** map accessTime to tileIdx */
    final TreeMap<Long, Integer> accessMap = new TreeMap<>();

    synchronized void put (int tileX, int tileY, DGraphTile tile){
        int tileIdx = DGraphTileFactory.getTileIdx(tileX,tileY);
        DGraphTile cacheTile = tileMap.put(tileIdx, tile);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
        }
        DTileConnector.connect(get( tileX-1,tileY), tile, true);
        DTileConnector.connect(tile, get( tileX+1,tileY), true);
        DTileConnector.connect(get( tileX,tileY-1), tile, false);
        DTileConnector.connect(tile, get( tileX,tileY+1), false);
        long now = System.nanoTime();
        tile.accessTime = now;
        accessMap.put(now, tileIdx);
        service2();
    }

    DGraphTile get(int tileX, int tileY){
        int tileIdx = DGraphTileFactory.getTileIdx(tileX,tileY);
        return get(tileIdx);
    }
    synchronized DGraphTile get(int tileIdx){
        DGraphTile cacheTile = tileMap.get(tileIdx);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
            long now = System.nanoTime();
            cacheTile.accessTime = now;
            if (!cacheTile.used)
                accessMap.put(now, tileIdx);
        }
        return cacheTile;
    }

    synchronized ArrayList<DGraphTile> getAll(){
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
        for (DGraphTile tile : tileMap.values()){
            accessMap.put(tile.accessTime, tile.tileIdx);
        }
        service2();
    }

    private void service2(){
        int initialSize = tileMap.size();
        while (tileMap.size() > limit){
            Map.Entry<Long, Integer> entry = accessMap.firstEntry();
            if ((entry != null) && (accessMap.size() >= 2)){ // accessMap.size() >= 2 ensures that the recently added tile isn't removed from cache
                DGraphTile tile = tileMap.get(entry.getValue());
                assert (tile != null);
                if (tile.used) {
                    accessMap.remove(tile.accessTime); // remove tile from access map, since cleanup is not allowed as long as it is used
                } else {
                    accessMap.remove(entry.getKey());
                    tileMap.remove(entry.getValue());
                    DTileConnector.disconnect(tile, Border.BORDER_NODE_WEST);
                    DTileConnector.disconnect(tile, Border.BORDER_NODE_NORTH);
                    DTileConnector.disconnect(tile, Border.BORDER_NODE_EAST);
                    DTileConnector.disconnect(tile, Border.BORDER_NODE_SOUTH);
                    tile.resetNodeRefs();
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
