package mg.mgmap.activity.mgmap.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import org.mapsforge.core.util.Parameters;

import mg.mgmap.generic.view.VUtil;

public class AlphaColorPicker extends LinearLayout {

    protected static int THUMB_COLOR = 0xFF606060;

    int alpha = 0;
    GradientDrawable gradientDrawable = null;
    BitmapDrawable bitmapDrawable = null;
    LayerDrawable layerDrawable = null;

    ColorPicker colorPicker = new ColorPicker(getContext()){
        @Override
        public void setCurrentColor(int color) {
            int transparentColor =  (color & 0x00FFFFFF) ;
            int alphaColor = ( transparentColor | ( alpha<<24 ));
            int fullColor = transparentColor | 0xFF000000;
            if (currentColor != alphaColor){ // color is changed
                super.setCurrentColor(alphaColor);
                gradientDrawable.setColors(new int[]{transparentColor, fullColor});
                onColorChanged(alphaColor);
            }
        }
    };

    SeekBar seekBar = new SeekBar(getContext());

    public AlphaColorPicker(Context context) {
        super(context);
        init();
    }

    public AlphaColorPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlphaColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public AlphaColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setInitialColor(int color){
        alpha = (color>>24) & 0xFF;
        seekBar.setProgress(alpha);
        colorPicker.setInitialColor(color);
    }
    public int getCurrentColor(){
        return colorPicker.currentColor;
    }



    @SuppressLint("ClickableViewAccessibility")
    private void init(){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(params);
        params.setMargins(VUtil.dp(20),VUtil.dp(2),VUtil.dp(20),VUtil.dp(20));
        setOrientation(VERTICAL);

        addView(colorPicker);

        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBar = new SeekBar(getContext());
        seekBar.setLayoutParams(llParams);
        seekBar.setMax(255);
        //sb.setScaleX(1.5f); does scale the thumb to a nice circle, but also scale the seekBar to the full width of the screen
        seekBar.setScaleY(1.5f);
        seekBar.setPadding(VUtil.dp(2),VUtil.dp(4),VUtil.dp(2),VUtil.dp(10));

        seekBar.getThumb().setColorFilter(THUMB_COLOR, PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(THUMB_COLOR, PorterDuff.Mode.SRC_IN);
        seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alpha = progress;
                colorPicker.setCurrentColor(colorPicker.getCurrentColor());
            }
        });

        gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {0,0});
        int x = VUtil.dp(8);
        int y = VUtil.dp(6);
        Bitmap bitmap = Bitmap.createBitmap(2*x, 2*y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(Parameters.ANTI_ALIASING);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setStrokeJoin(Paint.Join.ROUND);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xFFF0F0F0);
        canvas.drawRect(0,0,x,y, bgPaint);
        canvas.drawRect(x,y,2*x,2*y, bgPaint);
        bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        layerDrawable = new LayerDrawable(new Drawable[]{bitmapDrawable, gradientDrawable});

        seekBar.setBackground(layerDrawable);
        addView(seekBar);

        colorPicker.setCurrentColor(colorPicker.currentColor);
    }


    protected void onColorChanged(int color){}


}
