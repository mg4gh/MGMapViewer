package mg.mgmap.generic.graph.impl5;

import java.io.File;
import java.util.Set;

public interface DTilePersistentCache {

    void init(File cacheDir);

    void put(DGraphTile tile);

    boolean get(DGraphTile tile);

    void clear();

    void service();

}
