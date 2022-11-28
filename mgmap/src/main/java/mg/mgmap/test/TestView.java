package mg.mgmap.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.content.res.ResourcesCompat;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.NameUtil;

public class TestView extends RelativeLayout  {

    Point pxSize = new Point(); // size of the TestView in pixel
    int[] location = new int[2]; // location on screen - typically 0,0, but on Smartphone with notch e.g. 0,88

    ImageView cursor = null; // ImageView of the animated cursor
    ImageView click = null; // ImageView for animated click event (let this circle rise)

    Activity activity; // any activity (of this app)
    MGMapApplication application; // but the specific application -> provides access to TestControl

    public TestView(Context context) {
        super(context);
        init(context);
    }

    public TestView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);

    }

    private void init(Context context){
        if (context instanceof Activity) {
            activity = (Activity) context;
            if (activity.getApplication() instanceof MGMapApplication) {
                application = (MGMapApplication) activity.getApplication();
                application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbackAdapter(activity){
                    @Override
                    public void onActivityResumed(Activity activity) {
                        super.onActivityResumed(activity);
                        if (TestView.this.activity == activity){
                            application.getTestControl().onResume(TestView.this); // register this TestView at TestControl
                        }
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        super.onActivityPaused(activity);
                        if (TestView.this.activity == activity){
                            application.getTestControl().onPause(TestView.this); // unregister this TestView at TestControl
                            setVisibility(INVISIBLE, click);
                        }
                    }
                });
                createCursor(context);
                createClick(context);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) { // needed to determine the size of this TestView, but also the location
        super.onLayout(changed, l, t, r, b);
        this.getLocationOnScreen(location);
        pxSize.x = this.getWidth();
        pxSize.y = this.getHeight();
        if (Log.isLoggable(MGMapApplication.LABEL, Log.DEBUG)) {
            Log.d(MGMapApplication.LABEL, NameUtil.context() + " TestView size: " + pxSize + " "
                    + " TestView location: " + location[0] + "," + location[1] + " " + getContext().getClass().getSimpleName());
        }
        application.getTestControl().onTestViewLayout(this);
    }

    public void setVisibility(int visibility, View view){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            view.setVisibility(visibility);
        } else {
            activity.runOnUiThread(() -> setVisibility(visibility, view));
        }
    }

    public void createCursor(Context context){
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.cursor, context.getTheme());
        if (drawable != null){
            cursor = new ImageView(context);
            cursor.setImageDrawable(drawable);
            cursor.setVisibility(INVISIBLE);
            this.addView(cursor);
        }
    }
    public void createClick(Context context){
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.click, context.getTheme());
        if (drawable != null){
            click = new ImageView(context);
            click.setImageDrawable(drawable);
            click.setVisibility(INVISIBLE);
            this.addView(click);
        }
    }

    // convert position (in percent) to position in pixel
    public void percent2pos(PointF in, Point out){
        out.x = (int)(pxSize.x*in.x/100);
        out.y = (int)(pxSize.y*in.y/100);
    }
    // convert position (in percent) to position in raw pixel (needed e.g. for Clicker ... to do /system/bin/input tap x y t )
    public void percent2rawPos(PointF in, Point out){
        percent2pos(in, out);
        out.x += location[0];
        out.y += location[1];
    }
    // convert position (in pixel) to position for click drawable (left top corner of drawable, again in pixel)
    public void clickOffset(Point in, Point out){
        out.x = in.x - click.getWidth()/2;
        out.y = in.y - click.getHeight()/2;
    }

}
