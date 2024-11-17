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
package mg.mgmap.activity.mgmap.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;

import mg.mgmap.R;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.CC;

public class PointView extends MVLayer {

    private static final int DEFAULT_ZOOM_LEVEL = 15;

    private PointModel model;
    private final Paint paintStroke;
    private Paint paintFill = null;
    private float radius = 4;
    private float radiusIncrease = 1;

    private String text; // optional Text description
    private Paint paintText;
    private Paint paintTextBg;

    public PointView(PointModel model, Paint paintStroke){
        this.model = model;
        this.paintStroke = paintStroke;
    }
    public PointView(PointModel model, Paint paintStroke, Paint paintFill){
        this(model, paintStroke);
        this.paintFill = paintFill;
    }

    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.paintStroke == null && this.paintFill == null){
            return;
        }
        if ((model.getLat() == PointModel.NO_LAT_LONG) || (model.getLon() == PointModel.NO_LAT_LONG)){
            return;
        }

        int pixelX = lon2canvasX(model.getLon());
        int pixelY = lat2canvasY(model.getLat());

        int radiusInPixel = (int) (displayModel.getScaleFactor() * radius * getScale(zoomLevel));

        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersectsCircle(pixelX, pixelY, radiusInPixel)) {
            return;
        }

        if (this.paintStroke != null) {
            canvas.drawCircle(pixelX, pixelY, radiusInPixel, this.paintStroke);
        }
        if (this.paintFill != null) {
            canvas.drawCircle(pixelX, pixelY, radiusInPixel, this.paintFill);
        }


        if ((text != null) && (!text.isEmpty())){
            int textSize = getTextSize(radiusInPixel);
            paintText.setTextSize(textSize);
            paintTextBg.setTextSize(textSize);
            canvas.drawText(text, pixelX+radiusInPixel+textSize/4, pixelY+textSize/3, paintTextBg);
            canvas.drawText(text, pixelX+radiusInPixel+textSize/4, pixelY+textSize/3, paintText);
        }
    }

    protected float getScale(byte zoomLevel){
        return (float) Math.pow(this.radiusIncrease, zoomLevel - DEFAULT_ZOOM_LEVEL);
    }


    public PointModel getModel() {
        return model;
    }

    public void setModel(PointModel model) {
        this.model = model;
    }

    public float getRadius() {
        return radius;
    }

    public PointView setRadius(float radius) {
        this.radius = radius;
        return this;
    }
    @SuppressWarnings("UnusedReturnValue")
    public PointView setRadiusMeter(float meter){
        this.radius = (float)MercatorProjection.metersToPixels(meter, model.getLat(), MercatorProjection.getMapSize((byte)15, this.displayModel.getTileSize())) / displayModel.getScaleFactor();
        this.radiusIncrease = 2;
        return this;
    }

    public String getText() {
        return text;
    }

    public PointView setText(String text) {
        this.text = text;
        if (paintText == null){
            paintText = CC.getFillPaint(R.color.CC_WHITE);
            paintText.setColor(paintStroke.getColor());
            paintText.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
            paintTextBg = CC.getStrokePaint(R.color.CC_WHITE, 4);
            paintTextBg.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        }
        return this;
    }

    protected int getTextSize(int radiusInPixel){
        return radiusInPixel+radiusInPixel/3;
    }
}
