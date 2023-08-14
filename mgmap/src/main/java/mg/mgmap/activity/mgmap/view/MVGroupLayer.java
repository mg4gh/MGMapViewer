package mg.mgmap.activity.mgmap.view;

import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;

public class MVGroupLayer extends GroupLayer {

    public boolean onScroll(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.onScroll(scrollX1, scrollY1, scrollX2, scrollY2)) {
                return true;
            }
        }
        return false;
    }

}
