package mg.mgmap.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("unused")
public class TestView extends RelativeLayout  {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

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
            application = MGMapApplication.getByContext(context);
            createCursor(context);
            createClick(context);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) { // needed to determine the size of this TestView, but also the location
        super.onLayout(changed, l, t, r, b);
        this.getLocationOnScreen(location);
        pxSize.x = this.getWidth();
        pxSize.y = this.getHeight();
        application.getTestControl().onTestViewLayout(this);
        mgLog.d(" TestView size: " + pxSize +" TestView location: " + location[0] + "," + location[1] + " " + getContext().getClass().getSimpleName());
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

    public void setCursorPosition(Point positionOnScreen){
        cursor.setX(positionOnScreen.x - location[0]);
        cursor.setY(positionOnScreen.y - location[1]);
    }
    public void setClickPosition(Point positionOnScreen){
        click.setX(positionOnScreen.x - location[0] - click.getWidth()/2.0f);
        click.setY(positionOnScreen.y - location[1] - click.getHeight()/2.0f);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mgLog.d(activity.getClass().getSimpleName()+": Visibility="+visibility);
        if ( (visibility & (View.INVISIBLE | View.GONE)) == 0 ) { // is visible
            application.getTestControl().registerTestView(this);
        } else {
            application.getTestControl().unregisterTestView(this);
            click.setVisibility(INVISIBLE);
        }
    }

    public Activity getActivity() {
        return activity;
    }
}
