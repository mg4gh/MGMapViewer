package mg.mgmap.generic.graph;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import mg.mgmap.generic.util.basic.MGLog;

public class GGraphTileCache {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final ArrayList<Integer> workQueue = new ArrayList<>();
    private final LinkedHashMap<Integer, GGraphTile> cache = new LinkedHashMap<>(100, 0.6f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<Integer, GGraphTile> eldest) {
            boolean bRes = (size() > GGraphTileFactory.CACHE_LIMIT);
            if (bRes) {
                GGraphTile old = eldest.getValue();
                mgLog.d(() -> "remove from cache: tile x=" + old.tile.tileX + " y=" + old.tile.tileY + " Cache Size:" + cache.size());
            }
            return bRes;
        }
    };

    GGraphTileFactory gGraphTileFactory;
    public GGraphTileCache(GGraphTileFactory gGraphTileFactory){
        this.gGraphTileFactory = gGraphTileFactory;
        new Thread(() -> {
            while (true){
                try {
                    int key;
                    boolean load;
                    synchronized (this){
                        while (workQueue.size() == 0){
                            wait(1000);
                        }
                        key = workQueue.remove(0);
                        load = (cache.get(key) == null);
                    }
                    if (load){
                        int tileX = key>>16;
                        int tileY = key & 0xFFFF;
                        GGraphTile gGraphTile = gGraphTileFactory.loadGGraphTile(tileX,tileY);

                        synchronized (this){
                            cache.put(key, gGraphTile);
                            notifyAll();
                        }
                    }
                } catch (Throwable t){
                    mgLog.e(t);
                }
            }
        }).start();
    }

    synchronized GGraphTile get(byte originatorBorder, int tileX, int tileY){
        int key = GGraphTileFactory.getKey(tileX,tileY);
        GGraphTile gGraphTile;
        workQueue.add(0, key);
        if (originatorBorder != 0) triggerPrefetch(originatorBorder,tileX,tileY);
        notifyAll();
        while ((gGraphTile = cache.get(key)) == null){
            try {
                wait(1000);
            } catch (InterruptedException e) { mgLog.e(e); }
        }
        return gGraphTile;
    }

    synchronized void triggerPrefetch(byte originatorBorder, int tileX, int tileY) {
        int xMin = (GNode.deltaX(originatorBorder)==0)?-1:GNode.deltaX(originatorBorder);
        int xMax = (GNode.deltaX(originatorBorder)==0)? 1:GNode.deltaX(originatorBorder);
        int yMin = (GNode.deltaY(originatorBorder)==0)?-1:GNode.deltaX(originatorBorder);
        int yMax = (GNode.deltaY(originatorBorder)==0)? 1:GNode.deltaX(originatorBorder);
        for (int i=tileX+xMin; i<=tileX+xMax; i++){
            for (int j=tileY+yMin; j<=tileY+yMax; j++){
                workQueue.add(GGraphTileFactory.getKey(i,j));
            }
        }
    }


    synchronized ArrayList<GGraphTile> getAllTiles(){
        return new ArrayList<>(cache.values());
    }

    synchronized void clear(){
        cache.clear();
    }

    int size(){
        return cache.size();
    }
}
