package mg.mapviewer.features.search;

import android.view.KeyEvent;

import mg.mapviewer.model.PointModel;

public class SearchRequest {

    public String text;
    public int actionId;
    public long timestamp;
    public PointModel pos;
    public int zoom;

    public SearchRequest(String text, int actionId, long timestamp, PointModel pos, int zoom) {
        this.text = text;
        this.actionId = actionId;
        this.timestamp = timestamp;
        this.pos = pos;
        this.zoom = zoom;
    }
}
