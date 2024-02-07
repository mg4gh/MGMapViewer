package mg.mgmap.activity.mgmap.features.trad;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidBitmap;

import mg.mgmap.activity.mgmap.view.BitmapView;
import mg.mgmap.activity.mgmap.view.MVLayer;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public class TdMarker {

    private final ImageView tdvBgImage;

    private final ImageView tdvImage;

    private BitmapView tdMarkerLayer;

    private FSTrackDetails fstd;
    private final WriteablePointModel wpm = new WriteablePointModelImpl();

    private Bitmap bitmap;

    private final int dim;

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

    MVLayer registerTDService() {
        tdMarkerLayer = new BitmapView(bitmap, wpm, dim);
        return tdMarkerLayer;
    }
    void unregisterTDService() {
//            fstd.unregister(tdMarkerLayer); // not needed, since unregisterAll
        tdMarkerLayer = null;
    }

    public void setPosition(PointModel pm){
        wpm.setLat(pm.getLat());
        wpm.setLon(pm.getLon());
        wpm.setEle(pm.getEleA());

        if (wpm.getLaLo() == PointModelUtil.NO_POS){
            tdvImage.setVisibility(View.VISIBLE);
            tdMarkerLayer.setVisibility(false);
        } else {
            tdvImage.setVisibility(View.INVISIBLE);
            tdMarkerLayer.setVisibility(true);
        }
        fstd.redraw();
    }

    public PointModel getPosition(){
        return new PointModelImpl(wpm);
    }

    Runnable ttPositionTimeout = ()->{
        setPosition(new PointModelImpl());
        fstd.triggerRefreshObserver();
    };


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

    public Rect getMarkerRect(boolean fix){
        if (fix || (wpm.getLaLo() == PointModelUtil.NO_POS) || tdMarkerLayer == null){
            int[] loc = new int[2];
            tdvImage.getLocationOnScreen(loc);
            return new Rect(loc[0],loc[1],loc[0]+dim,loc[1]+dim);
        } else {
            return tdMarkerLayer.getDrawableRect();
        }
    }
}
