package mg.mgmap.generic.graph;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.generic.util.basic.MGLog;

public class GTileCache {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final int limit;
    public GTileCache(int limit){
        this.limit = limit;
    }

    TreeMap<Integer, GGraphTile> tileMap = new TreeMap<>();
    TreeMap<Long, Integer> accessMap = new TreeMap<>();

    synchronized void put(int tileIdx, GGraphTile tile){
        GGraphTile cacheTile = tileMap.put(tileIdx, tile);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
        }
        long now = System.nanoTime();
        tile.accessTime = now;
        accessMap.put(now, tileIdx);
        service();
    }

    synchronized GGraphTile get(int tileIdx){
        GGraphTile cacheTile = tileMap.get(tileIdx);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
            long now = System.nanoTime();
            cacheTile.accessTime = now;
            accessMap.put(now, tileIdx);
        }
        return cacheTile;
    }

    synchronized ArrayList<GGraphTile> getAll(){
        return new ArrayList<>(tileMap.values());
    }

    synchronized void clear(){
        tileMap.clear();
        accessMap.clear();
    }

    synchronized int size(){
        return tileMap.size();
    }

    void service(){
        int initialSize = tileMap.size();
        while (tileMap.size() > limit){
            Map.Entry<Long, Integer> firstAccessEntry = accessMap.firstEntry();
            if (firstAccessEntry != null){
                GGraphTile oldestTile = tileMap.get(firstAccessEntry.getValue());
                assert (oldestTile != null);
                if (oldestTile.used) {
                    break;
                } else {
                    accessMap.remove(firstAccessEntry.getKey());
                    tileMap.remove(firstAccessEntry.getValue());
                    cleanupNeighbourTile(oldestTile,GNode.BORDER_NODE_WEST);
                    cleanupNeighbourTile(oldestTile,GNode.BORDER_NODE_NORTH);
                    cleanupNeighbourTile(oldestTile,GNode.BORDER_NODE_EAST);
                    cleanupNeighbourTile(oldestTile,GNode.BORDER_NODE_SOUTH);
                }
            } else {
                mgLog.e("tileMap.size()="+tileMap.size()+" accessMap.size()="+accessMap.size());
                break;
            }
        }
        if (accessMap.size() != initialSize){
            mgLog.d("cleanup initialSize="+initialSize + " currentSize="+tileMap.size());
        }
    }

    private void cleanupNeighbourTile(GGraphTile gGraphTile, byte border){
        GGraphTile neighbourTile = gGraphTile.neighbourTiles[border];
        if (neighbourTile != null){
            neighbourTile.dropNeighboursToTile(gGraphTile.tileIdx, border);
        }
    }

}
