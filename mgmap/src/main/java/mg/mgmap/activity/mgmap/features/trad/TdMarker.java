package mg.mgmap.activity.mgmap.features.trad;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidBitmap;

import mg.mgmap.activity.mgmap.view.BitmapView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public class TdMarker {

    private final ImageView tdvBgImage;

    private final ImageView tdvImage;

    private BitmapView tdMarkerLayer;

    private final FSTrackDetails fstd;
    private final WriteablePointModel wpm = new WriteablePointModelImpl();

    private Bitmap bitmap;

    private int dim;

    public TdMarker(FSTrackDetails fstd, RelativeLayout tdView, boolean firstMarker, int totalWidth, int imgDim){
        this.fstd = fstd;
        this.dim = imgDim;

        {
            tdvBgImage = new ImageView(fstd.getApplication());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dim, dim);
            params.setMargins(firstMarker?0:totalWidth-dim,0,0,0);
            tdvBgImage.setLayoutParams(params);
            tdView.addView(tdvBgImage);
        }
        {
            tdvImage = new ImageView(fstd.getApplication());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dim, dim);
            params.setMargins(firstMarker?0:totalWidth-dim,0,0,0);
            tdvImage.setLayoutParams(params);
            tdView.addView(tdvImage);
        }

    }

    public BitmapView getMarkerLayer(){
        return tdMarkerLayer;
    }

    public void setPoint(PointModel pm){
        wpm.setLat(pm.getLat());
        wpm.setLon(pm.getLon());

        if ((wpm.getLat() == PointModel.NO_LAT_LONG) && (wpm.getLon() == PointModel.NO_LAT_LONG)){
            tdvImage.setVisibility(View.VISIBLE);
            if (tdMarkerLayer != null){
                fstd.unregister(tdMarkerLayer);
            }
        } else {
            tdvImage.setVisibility(View.INVISIBLE);
            if (tdMarkerLayer == null){
                tdMarkerLayer = new BitmapView(bitmap, wpm);
                fstd.register(tdMarkerLayer);
            } else {
                fstd.redraw();
            }
        }
    }

    public void refresh(){
        setPoint(wpm);
    }

    public void resetPosition(){
        wpm.setLat(PointModel.NO_LAT_LONG);
        wpm.setLon(PointModel.NO_LAT_LONG);
    }

    public void setDrawable(Drawable drawableBg, Drawable drawableFg) {
        tdvBgImage.setImageDrawable(drawableBg);
        tdvBgImage.setVisibility(View.VISIBLE);
        tdvImage.setImageDrawable(drawableFg);
        tdvImage.setVisibility(View.VISIBLE);

        if (drawableFg != null){
            android.graphics.Bitmap aBitmap = android.graphics.Bitmap.createBitmap(dim,dim,android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas aCanvas = new android.graphics.Canvas(aBitmap);
            drawableFg.setBounds(0,0,dim,dim);
            drawableFg.draw(aCanvas);
            bitmap = new AndroidBitmap((aBitmap));
        } else {
            bitmap = null;
        }
    }

    public Rect getMarkerRect(){
        if ((wpm.getLat() == PointModel.NO_LAT_LONG) && (wpm.getLon() == PointModel.NO_LAT_LONG)){
            int[] loc = new int[2];
            tdvImage.getLocationOnScreen(loc);
            return new Rect(loc[0],loc[1],loc[0]+dim,loc[1]+dim);
        } else {
            return tdMarkerLayer.getDrawableRect();
        }
    }
}
