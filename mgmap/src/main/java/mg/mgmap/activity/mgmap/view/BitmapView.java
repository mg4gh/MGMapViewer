package mg.mgmap.activity.mgmap.view;

import android.graphics.Rect;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import mg.mgmap.generic.model.PointModel;

public class BitmapView extends MVLayer{


    private boolean visibility = true;
    private final PointModel pm;
    private final Bitmap bitmap;
    private final int dim;

    public BitmapView(Bitmap bitmap, PointModel pm, int dim){
        this.bitmap = bitmap;
        this.pm = pm;
        this.dim = dim;
    }

    @Override
    protected void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        super.doDraw(boundingBox, zoomLevel, canvas, topLeftPoint);
        if (visibility && (bitmap != null)){
            int x = lon2canvasX(pm.getLon());
            int y = lat2canvasY(pm.getLat());
            int left = x - dim/2;
            int top = y - dim;
            canvas.drawBitmap(bitmap,left, top);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public Rect getDrawableRect(){
        if (topLeftPoint == null) return new Rect(1,1,0,0);
        int x = lon2x(pm.getLon());
        int y = lat2y(pm.getLat());
        int left = x - dim/2;
        int top = y - dim;
        int right = x + dim/2;
        int bottom = y;
        return new Rect(left,top,right,bottom);
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}
