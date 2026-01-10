package mg.mgmap.activity.mgmap.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import org.mapsforge.core.util.Parameters;

import mg.mgmap.generic.view.VUtil;

public class ColorPicker extends View {

    final public static int[] colors = new int[]{
            0xFFFF0000, 0xFFFF4000, 0xFFFF8000, 0xFFFFC000, 0xFFFFFF00, 0xFFC0FF00, 0xFF80FF00, 0xFF40FF00,
            0xFF00FF00, 0xFF00FF40, 0xFF00FF80, 0xFF00FFC0, 0xFF00FFFF, 0xFF00C0FF, 0xFF0080FF, 0xFF0040FF,
            0xFF0000FF, 0xFF4000FF, 0xFF8000FF, 0xFFC000FF, 0xFFFF00FF, 0xFFFF00C0, 0xFFFF0080, 0xFFFF0040 };
    final public static Paint[] paints = new Paint[colors.length];

    static {
        for (int i=0; i<colors.length; i++){
            Paint paint = new Paint();
            paint.setAntiAlias(Parameters.ANTI_ALIASING);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i]);
            paints[i] = paint;
        }
    }



    Point[] points = new Point[colors.length];
    float radius = 0;
    int initialColor = 0;
    Paint initialPaint = null;
    int currentColor = 0;
    Paint currentPaint = null;


    public ColorPicker(Context context) {
        super(context);
        init();
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setInitialColor(int color){
        initialColor = color;
        initialPaint.setColor(color);
        setCurrentColor(color);
    }

    public int getCurrentColor(){
        return currentColor;
    }
    public void setCurrentColor(int color){
        currentColor = color;
        currentPaint.setColor(currentColor);
        this.invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUtil.dp(350));
        this.setLayoutParams(params);

        initialPaint = new Paint();
        initialPaint.setAntiAlias(Parameters.ANTI_ALIASING);
        initialPaint.setStrokeCap(Paint.Cap.ROUND);
        initialPaint.setStrokeJoin(Paint.Join.ROUND);
        initialPaint.setStyle(Paint.Style.FILL);

        currentPaint = new Paint();
        currentPaint.setAntiAlias(Parameters.ANTI_ALIASING);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStyle(Paint.Style.FILL);

        for (int i=0; i<colors.length; i++) {
            points[i] = new Point();
        }

        setOnTouchListener((v,e)->{
            if (e.getAction() == MotionEvent.ACTION_DOWN){
                int bestIdx = -1;
                double bestDistanceSqr = Double.MAX_VALUE;
                for (int i=0; i<colors.length; i++){
                    double distSqr = Math.pow(e.getX() - points[i].x, 2) + Math.pow(e.getY() - points[i].y, 2);
                    if ((distSqr < bestDistanceSqr) && (distSqr <= Math.pow(radius/10*1.2 ,2))){
                        bestDistanceSqr = distSqr;
                        bestIdx = i;
                    }
                }
                if (bestIdx >= 0){
                    setCurrentColor(colors[bestIdx]);
                }
                invalidate();
            }
            return false;
        });
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        float dx  = getWidth();
        float dy = getHeight();

        // center
        float cx = dx/2;
        float cy = dy/2;

        // radius 40% (diameter 80%)
        radius = (Math.min(dx,dy) *40) / 100;

        float r = radius/3;
        RectF rectF = new RectF(cx-r,cy-r,cx+r,cy+r);
        canvas.drawArc(rectF, 270, 180, true, currentPaint);
        canvas.drawArc(rectF, 90, 180, true, initialPaint);

        double step = 2 * Math.PI / colors.length;
        for (int i=0; i<colors.length; i++){
            double px = Math.sin(i*step) * radius;
            double py = Math.cos(i*step) * radius;
            points[i].set((int)(cx+(float)px), (int)(cy+(float) py));

            canvas.drawCircle(points[i].x, points[i].y, radius/10, paints[i]);
        }
    }

}
