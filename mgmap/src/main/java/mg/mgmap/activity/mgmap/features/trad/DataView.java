package mg.mgmap.activity.mgmap.features.trad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.annotation.NonNull;

import mg.mgmap.generic.model.PointModelUtil;

@SuppressLint("ViewConstructor")
public class DataView extends View {

    Path path = new Path();
    float dataStep;
    int width;
    int height;


    Paint paint = new Paint();


    public DataView(Context context, float dataStep, int width, int height) {
        super(context);
        this.dataStep = dataStep;
        this.width = width;
        this.height = height;
        paint.setColor(0x80FFFFFF);
        paint.setStyle(Paint.Style.FILL);
        setBackgroundColor(0x40FFFFFF);
    }


    public void setData(float[] data) {
        float dataMin = Float.MAX_VALUE;
        float dataMax = Float.MIN_VALUE;
        for (float d : data){
            if (d > dataMax) dataMax = d;
            if (d < dataMin) dataMin = d;
        }

        dataMin = ((int)(dataMin / dataStep)) * dataStep;
        dataMax = ((int)(dataMax / dataStep) + 1) * dataStep;

        path.reset();
        path.moveTo(0,height);
        for (int idx=0; idx<data.length; idx++){
            path.lineTo(idx*width/(data.length-1f), height - (float)PointModelUtil.interpolate(dataMin, dataMax, 0, height, data[idx]));
        }
        path.lineTo(width,height);
        path.lineTo(0,height);
        path.close();
        this.invalidate();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (path != null){
            canvas.drawPath(path, paint);
        }
    }
}
