package mg.mgmap.activity.mgmap.features.trad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.view.VUtil;

@SuppressLint("ViewConstructor")
public class DataView extends View {

    private static final int BG_COLOR = 0x40FFFFFF;
    private static final int FG_COLOR = 0x80FFFFFF;
    private static final int STEP_COLOR = 0x80FFFFFF;
    private static final int TEXT_BG_COLOR = 0xFFFFFFFF;
    private int textColor = 0x00000000;


    final Path pathFg = new Path();
    final Path pathStep = new Path();
    final ArrayList<AText> texts = new ArrayList<>();
    final Path pathPos = new Path();

    final float dataStep;
    final int width;
    final int height;


    final Paint paintFg;
    final Paint paintStep;
    final TextPaint paintText;
    final TextPaint paintBgText;
    final Paint paintPos;


    private static class AText{
        final String text;
        final float x;
        final float y;
        final TextPaint paint;

        public AText(String text, float x, float y, TextPaint paint) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.paint = paint;
        }
    }


    public DataView(Context context, float dataStep, int width, int height) {
        super(context);
        this.dataStep = dataStep;
        this.width = width;
        this.height = height;
        paintFg = new Paint();
        paintFg.setColor(FG_COLOR);
        paintFg.setStyle(Paint.Style.FILL);
        paintStep = new Paint();
        paintStep.setColor(STEP_COLOR);
        paintStep.setStrokeWidth(3);
        paintStep.setStyle(Paint.Style.STROKE);
        paintText = new TextPaint();
        paintText.setStyle(Paint.Style.FILL);
        paintText.setTextSize(VUtil.dp(15));
        paintText.setStrokeWidth(3);
        paintText.setTypeface(Typeface.DEFAULT_BOLD);
        paintBgText = new TextPaint();
        paintBgText.setColor(TEXT_BG_COLOR);
        paintBgText.setStyle(Paint.Style.FILL_AND_STROKE);
        paintBgText.setTextSize(VUtil.dp(15));
        paintBgText.setStrokeWidth(5);
        paintBgText.setTypeface(Typeface.DEFAULT_BOLD);
        setBackgroundColor(BG_COLOR);

        paintPos = new Paint();
        paintPos.setStrokeWidth(4);
        paintPos.setStyle(Paint.Style.STROKE);

    }


    public void setData(float[] data, float pos) {
        pathFg.reset();
        pathStep.reset();
        texts.clear();
        pathPos.reset();
        if ((data != null) && (data.length>=2)){
            float dataMin = Float.MAX_VALUE;
            float dataMax = Float.MIN_VALUE;
            for (float d : data){
                if (d > dataMax) dataMax = d;
                if (d < dataMin) dataMin = d;
            }

            dataMin = ((int)(dataMin / dataStep)) * dataStep;
            dataMax = ((int)(dataMax / dataStep) + 1) * dataStep;

            pathFg.moveTo(0,height);
            for (int idx=0; idx<data.length; idx++){
                pathFg.lineTo(idx*width/(data.length-1f), height - (float)PointModelUtil.interpolate(dataMin, dataMax, 0, height, data[idx]));
            }
            pathFg.lineTo(width,height);
            pathFg.lineTo(0,height);
            pathFg.close();

            float currentDataStep = dataStep;
            float interval = dataMax - dataMin;
            while (interval / currentDataStep < 3.01f) currentDataStep /= 2;
            while (interval / currentDataStep > 6.99f) currentDataStep *= 2;
            boolean textEachInterval = (interval / currentDataStep < 3.01f);

            paintText.setColor(textColor);
            boolean odd = true;
            for (float current=dataMin+currentDataStep; current<dataMax-currentDataStep*0.1; current+=currentDataStep ){
                float y = (float)(height - PointModelUtil.interpolate(dataMin, dataMax, 0,height, current));
                pathStep.moveTo(0,y);
                pathStep.lineTo(width,y);

                if (textEachInterval || odd){
                    String text = (int)current+"m";
                    texts.add(new AText(text, VUtil.dp(2), y + VUtil.dp(4), paintBgText));
                    texts.add(new AText(text, VUtil.dp(2), y + VUtil.dp(4), paintText));
                }
                odd = !odd;
            }

            if (pos > 0){
                paintPos.setColor(textColor);
                float x = pos*width/(data.length-1f);
                pathPos.moveTo(x,0);
                pathPos.lineTo(x,height);
                pathPos.lineTo(x,0);
                pathPos.close();
            }
        }

        this.invalidate();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(pathFg, paintFg);
        canvas.drawPath(pathStep, paintStep);
        for (AText aText : texts){
            canvas.drawText(aText.text, aText.x,aText.y,aText.paint);
        }
        canvas.drawPath(pathPos, paintPos);
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
}
