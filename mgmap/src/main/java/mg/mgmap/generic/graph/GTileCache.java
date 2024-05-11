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

    synchronized void put (int tileX, int tileY, GGraphTile tile){
        int tileIdx = GGraphTileFactory.getKey(tileX,tileY);
        GGraphTile cacheTile = tileMap.put(tileIdx, tile);
        if (cacheTile != null){
            accessMap.remove(cacheTile.accessTime);
        }
        GTileConnector.connect(get( tileX-1,tileY), tile, true);
        GTileConnector.connect(tile, get( tileX+1,tileY), true);
        GTileConnector.connect(get( tileX,tileY-1), tile, false);
        GTileConnector.connect(tile, get( tileX,tileY+1), false);
        long now = System.nanoTime();
        tile.accessTime = now;
        accessMap.put(now, tileIdx);
        service();
    }

    synchronized GGraphTile get(int tileX, int tileY){
        int tileIdx = GGraphTileFactory.getKey(tileX,tileY);
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
                    GTileConnector.disconnect(oldestTile,GNode.BORDER_NODE_WEST);
                    GTileConnector.disconnect(oldestTile,GNode.BORDER_NODE_NORTH);
                    GTileConnector.disconnect(oldestTile,GNode.BORDER_NODE_EAST);
                    GTileConnector.disconnect(oldestTile,GNode.BORDER_NODE_SOUTH);
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

}
