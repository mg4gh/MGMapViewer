package mg.mgmap.activity.mgmap.view;

import android.graphics.Rect;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import java.util.Locale;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.VUtil;

public class BitmapView extends MVLayer{


    private final PointModel pm;
    private final Bitmap bitmap;

    public BitmapView(Bitmap bitmap, PointModel pm){
        this.bitmap = bitmap;
        this.pm = pm;
    }

    @Override
    protected void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        super.doDraw(boundingBox, zoomLevel, canvas, topLeftPoint);
        if (bitmap != null){
            int x = lon2canvasX(pm.getLon());
            int y = lat2canvasY(pm.getLat());
            int left = x - VUtil.dp(25);
            int top = y - VUtil.dp(50);
            canvas.drawBitmap(bitmap,left, top);
            MGLog.sd(String.format(Locale.ENGLISH," (%d,%d) (%.6f,%.6f) (%.1f,%.1f)",x,y,pm.getLat(),pm.getLon(),topLeftPoint.x,topLeftPoint.y));
        }
    }

    public Rect getDrawableRect(){
        if (topLeftPoint == null) return new Rect(1,1,0,0);
        int x = lon2x(pm.getLon());
        int y = lat2y(pm.getLat());
        int left = x - VUtil.dp(25);
        int top = y - VUtil.dp(50);
        int right = x + VUtil.dp( 25);
        int bottom = y;
        return new Rect(left,top,right,bottom);
    }

}
