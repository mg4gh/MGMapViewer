package mg.mapviewer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {


    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void fling(int velocityY) {
        double v = velocityY;
        double f =  Math.max( Math.abs(v/4000.0) , 1);

        int nv = (int)(v*f);
        Log.i("MyScroll", "fling "+velocityY+" "+nv);
        super.fling(nv);
    }
}
