package mg.mgmap.activity.mgmap.features.trad;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidBitmap;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.view.BitmapView;
import mg.mgmap.activity.mgmap.view.MVLayer;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

public class TdMarker {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final ImageView tdvBgImage; // bg image in tdView
    private final ImageView tdvImage; // fg image in tdView
    private ImageView animationImage; // animation image is the same as fg image, it is used for home animation of Marker
    private RelativeLayout animationGroup;

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
        {
            RelativeLayout animationView = fstd.getActivity().findViewById(R.id.animationview);
            animationGroup = new RelativeLayout(fstd.getApplication());
            animationGroup.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            animationView.addView(animationGroup);
            animationImage = new ImageView(fstd.getApplication());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dim, dim);
            params.setMargins(0,0,0,0);
            animationImage.setLayoutParams(params);
        }

    }

    MVLayer registerTDService() {
        tdMarkerLayer = new BitmapView(bitmap, wpm, dim);
        return tdMarkerLayer;
    }
    void unregisterTDService() { }

    public void setPosition(PointModel pm){
        wpm.setLat(pm.getLat());
        wpm.setLon(pm.getLon());
        wpm.setEle(pm.getEle());

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

    final Runnable homeAnimation = ()->{
        AutoTransition at = new AutoTransition();
        at.setDuration(FSTrackDetails.MARKER_ANIMATION_DURATION);
        at.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                mgLog.d("transition end");
                setPosition(new PointModelImpl());
                fstd.triggerRefreshObserver();
                animationGroup.removeView(animationImage);
            }
            @Override
            public void onTransitionCancel(Transition transition) {
                mgLog.d("transition cancel");
                this.onTransitionEnd(transition);
            }
        });
        TransitionManager.beginDelayedTransition(animationGroup, at);
        setAnimationViewPosition(getTdvImageRect());
    };

    final Runnable ttPositionTimeout = ()->{
        if (tdMarkerLayer.getVisibility()){
            assert (tdMarkerLayer.getVisibility());
            tdMarkerLayer.setVisibility(false);
            setAnimationViewPosition(tdMarkerLayer.getDrawableRect());
            if (animationImage.getParent() == null){ // theoretically always true, practically at least once not -> caused crash.
                animationGroup.addView(animationImage);
            }
            fstd.redraw();
            FSTrackDetails.getTimer().postDelayed(homeAnimation , 50);
        }
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
        animationImage.setImageDrawable(drawableFg);
        animationImage.setVisibility(View.VISIBLE);

        if (drawableFg != null){
            android.graphics.Bitmap aBitmap = android.graphics.Bitmap.createBitmap(dim,dim,android.graphics.Bitmap.Config.ARGB_8888);
            Canvas aCanvas = new Canvas(aBitmap);
            drawableFg.setBounds(0,0,dim,dim);
            drawableFg.draw(aCanvas);
            bitmap = new AndroidBitmap((aBitmap));
        } else {
            bitmap = null;
        }
    }

    public Rect getMarkerRect(boolean fix){
        if (fix || (wpm.getLaLo() == PointModelUtil.NO_POS) || tdMarkerLayer == null){
            return getTdvImageRect();
        } else {
            return tdMarkerLayer.getDrawableRect();
        }
    }

    private Rect getTdvImageRect(){
        int[] loc = new int[2];
        tdvImage.getLocationOnScreen(loc);
        return new Rect(loc[0],loc[1],loc[0]+dim,loc[1]+dim);
    }

    private void setAnimationViewPosition(Rect pos){
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)animationImage.getLayoutParams();
        params.setMargins(pos.left,pos.top,0,0);
        animationImage.setLayoutParams(params);
    }

}
