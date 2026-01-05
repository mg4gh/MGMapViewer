/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.util;

import android.content.Context;
import android.graphics.Color;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Utility for ColorConstants.
 */
public class CC { // short for ColorConstant

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
    private static final Map<Integer, Integer> colorMap = new HashMap<>();

    public static void init(Context context){
        if (context != null) {
            try {
                colorMap.put(R.color.CC_BLACK, context.getColor(R.color.CC_BLACK));
                colorMap.put(R.color.CC_BLACK_A150, context.getColor(R.color.CC_BLACK_A150));
                colorMap.put(R.color.CC_BLACK_A20, context.getColor(R.color.CC_BLACK_A20));
                colorMap.put(R.color.CC_WHITE, context.getColor(R.color.CC_WHITE));
                colorMap.put(R.color.CC_WHITE_A150, context.getColor(R.color.CC_WHITE_A150));
                colorMap.put(R.color.CC_WHITE_A100, context.getColor(R.color.CC_WHITE_A100));
                colorMap.put(R.color.CC_RED, context.getColor(R.color.CC_RED));
                colorMap.put(R.color.CC_RED_A50, context.getColor(R.color.CC_RED_A50));
                colorMap.put(R.color.CC_RED_A150, context.getColor(R.color.CC_RED_A150));
                colorMap.put(R.color.CC_RED100_A100, context.getColor(R.color.CC_RED100_A100));
                colorMap.put(R.color.CC_RED100_A150, context.getColor(R.color.CC_RED100_A150));
                colorMap.put(R.color.CC_GREEN, context.getColor(R.color.CC_GREEN));
                colorMap.put(R.color.CC_GREEN100_A100, context.getColor(R.color.CC_GREEN100_A100));
                colorMap.put(R.color.CC_GREEN150_A150, context.getColor(R.color.CC_GREEN150_A150));
                colorMap.put(R.color.CC_ORANGE_A50, context.getColor(R.color.CC_ORANGE_A50));
                colorMap.put(R.color.CC_ORANGE, context.getColor(R.color.CC_ORANGE));
                colorMap.put(R.color.CC_GRAY200, context.getColor(R.color.CC_GRAY200));
                colorMap.put(R.color.CC_GRAY240, context.getColor(R.color.CC_GRAY240));
                colorMap.put(R.color.CC_GRAY200_A150, context.getColor(R.color.CC_GRAY200_A150));
                colorMap.put(R.color.CC_GRAY100_A100, context.getColor(R.color.CC_GRAY100_A100));
                colorMap.put(R.color.CC_GRAY100_A150, context.getColor(R.color.CC_GRAY100_A150));
                colorMap.put(R.color.CC_BLUE, context.getColor(R.color.CC_BLUE));
                colorMap.put(R.color.CC_BLUE_A50, context.getColor(R.color.CC_BLUE_A50));
                colorMap.put(R.color.CC_BLUE_A150, context.getColor(R.color.CC_BLUE_A150));
                colorMap.put(R.color.CC_BLUE100_A100, context.getColor(R.color.CC_BLUE100_A100));
                colorMap.put(R.color.CC_BLUE150_A150, context.getColor(R.color.CC_BLUE150_A150));
                colorMap.put(R.color.CC_PURPLE, context.getColor(R.color.CC_PURPLE));
                colorMap.put(R.color.CC_PURPLE_A100, context.getColor(R.color.CC_PURPLE_A100));
                colorMap.put(R.color.CC_PURPLE_A150, context.getColor(R.color.CC_PURPLE_A150));
                colorMap.put(R.color.CC_PINK, context.getColor(R.color.CC_PINK));



            } catch (Exception e) {
                mgLog.e(e);
                throw new RuntimeException(e);
            }

        }
    }

    public static int getColor(int colorId){
        Integer color = colorMap.get(colorId);
        if (color == null){
            mgLog.w(String.format("color resource not found: 0x%08X",colorId));
            color = 0;
        }
        return color;
    }

    public static Paint getStrokePaint(int colorId, float width){
        return getStrokePaint4Color(getColor(colorId), width);
    }

    public static Paint getStrokePaint4Color(int color, float width){
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor( color );
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        return paint;
    }

    public static Paint getFillPaint(int colorId){
        return getFillPaint4Color(getColor(colorId));
    }
    public static Paint getFillPaint4Color(int color){
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor( color );
        paint.setStrokeWidth(0);
        paint.setStyle(Style.FILL);
        return paint;
    }

    public static int addAlpha(int color, float alpha){
        float currentAlpha = ((color >> 24) & 0xFF) / 255.0f;
        float newAlpha = currentAlpha * alpha;
        return (color & 0x00FFFFFF) | (floatAlpha2int(newAlpha)<<24);
    }

    public static Paint getAlphaClone(Paint opaint, float fAlpha){
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor( addAlpha( opaint.getColor(), fAlpha ));
        paint.setStrokeWidth(opaint.getStrokeWidth());
        paint.setStyle(Style.STROKE);
        return paint;
    }
    public static Paint getAlphaCloneFill(Paint opaint, float fAlpha){
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor( addAlpha( opaint.getColor(), fAlpha ));
        paint.setStrokeWidth(opaint.getStrokeWidth());
        paint.setStyle(Style.FILL);
        return paint;
    }

    private static final Map<Integer, Paint> glPaints = new HashMap<>();

    public static void initGlPaints(float fAlpha){
        int a = floatAlpha2int(fAlpha);
        if (!glPaints.isEmpty()){
            if (Color.alpha(Objects.requireNonNull(glPaints.get(0)).getColor()) == a) return; // alpha unchanged
        }
        int w=6;
        {
            int steps = 30;
            for (int i=1; i<=steps; i++){
                int r=0;
                int g= 255 - (i*255)/steps;
                int b=255;
                int c = argb(a,r,g,b);
                glPaints.put(-10-i,getStrokePaint4Color(c, w));
            }
        }
        {
            int steps = 10;
            for (int i=1; i<=steps; i++){
                int r=0;
                int g=255;
                int b=(i*255)/steps;
                int c = argb(a,r,g,b);
                glPaints.put(-i,getStrokePaint4Color(c, w));
            }
        }
        glPaints.put(0,getStrokePaint4Color((a<<24) +0x00FF00, w));
        {
            int steps = 10;
            for (int i=1; i<=steps; i++){
                int r=(i*255)/steps;
                int g=255;
                int b=0;
                int c = argb(a,r,g,b);
                glPaints.put(i,getStrokePaint4Color(c, w));
            }
        }
        {
            int steps = 30;
            for (int i=1; i<=steps; i++){
                int r=255;
                int g= 255 - (i*255)/steps;
                int b=0;
                int c = argb(a,r,g,b);
                glPaints.put(10+i,getStrokePaint4Color(c, w));
            }
        }
    }

    public static int argb(int a, int r, int g, int b){
        return (a<<24) + (r<<16) + (g<<8) + b;
    }


    public static Paint getGlPaint(float glValue){
        if (glPaints.isEmpty()){ // init paint with default alpha, if not yet done.
            initGlPaints(0xC0);
        }
        if (glValue > 20) glValue = 20;
        if (glValue < -20) glValue = -20;
        int glIndex = (int)(glValue*2); // each color corresponds to a half percent
        return glPaints.get(glIndex);
    }

    public static int floatAlpha2int(float fAlpha){
        return (int)(fAlpha*255);
    }

}
