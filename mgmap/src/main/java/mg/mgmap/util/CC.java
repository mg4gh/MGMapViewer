/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mgmap.util;

import android.app.Activity;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for ColorConstants.
 */
public class CC { // short for ColorConstant

    private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
    private static Activity activity;

    public static void setActivity(Activity activity){
        CC.activity = activity;
    }


    public static int getColor(int colorId){
        return ContextCompat.getColor(activity.getApplicationContext(),colorId);
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
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor( getColor(colorId) );
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

    private static Map<Integer, Paint> glPaints = new HashMap<>();

    public static void initGlPaints(float fAlpha){
        int a = floatAlpha2int(fAlpha);
        if (glPaints.size() > 0){
            if (Color.alpha(glPaints.get(0).getColor()) == a) return; // alpha unchanged
        }
        int w=6;
//        int a=0xC0;
        int steps = 30;
        for (int i=1; i<=steps; i++){
            int r=0;
            int g= 255 - (i*255)/steps;
            int b=255;
            int c = (a<<24) + (r<<16) + (g<<8) + b;
            glPaints.put(-10-i,getStrokePaint4Color(c, w));
        }
        steps = 10;
        for (int i=1; i<=steps; i++){
            int r=0;
            int g=255;
            int b=(i*255)/steps;
            int c = (a<<24) + (r<<16) + (g<<8) + b;
            glPaints.put(-i,getStrokePaint4Color(c, w));
        }

        glPaints.put(0,getStrokePaint4Color((a<<24) +0x00FF00, w));
        steps = 10;
        for (int i=1; i<=steps; i++){
            int r=(i*255)/steps;
            int g=255;
            int b=0;
            int c = (a<<24) + (r<<16) + (g<<8) + b;
            glPaints.put(i,getStrokePaint4Color(c, w));
        }
        steps = 30;
        for (int i=1; i<=steps; i++){
            int r=255;
            int g= 255 - (i*255)/steps;
            int b=0;
            int c = (a<<24) + (r<<16) + (g<<8) + b;
            glPaints.put(10+i,getStrokePaint4Color(c, w));
        }

    }


    public static Paint getGlPaint(float glValue){
        if (glPaints.size() == 0){ // init paint with default alpha, if not yet done.
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