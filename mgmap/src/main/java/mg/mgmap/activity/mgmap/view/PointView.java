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
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;

import mg.mgmap.generic.model.PointModel;

public class PointView extends MVLayer {

    private static final int DEFAULT_ZOOM_LEVEL = 15;

    private PointModel model;
    private Paint paintStroke;
    private Paint paintFill = null;
    private float radius = 4;
    private float radiusIncrease = 1;
    private float radiusMeter = -1;


    public PointView(PointModel model, Paint paintStroke){
        this.model = model;
        this.paintStroke = paintStroke;
    }
    public PointView(PointModel model, Paint paintStroke, Paint paintFill){
        this(model, paintStroke);
        this.paintFill = paintFill;
    }
    public PointView(PointModel model, Paint paintStroke, Paint paintFill, boolean keepAligned){
        this(model, paintStroke, paintFill);
    }


    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.paintStroke == null && this.paintFill == null){
            return;
        }
        if ((model.getLat() == PointModel.NO_LAT_LONG) || (model.getLon() == PointModel.NO_LAT_LONG)){
            return;
        }

        int pixelX = lon2x(model.getLon());
        int pixelY = lat2y(model.getLat());

        int radiusInPixel = (int) (displayModel.getScaleFactor() * radius * getScale(zoomLevel));
//        Log.v(MGMapApplication.LABEL, NameUtil.context()+" pixelX="+pixelX+" pixelY="+pixelY+" pos="+String.format("%.6f,%.6f",model.getLat(),model.getLon()));

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
    public PointView setRadiusMeter(float meter){
        this.radius = (float)MercatorProjection.metersToPixels(meter, model.getLat(), MercatorProjection.getMapSize((byte)15, this.displayModel.getTileSize())) / displayModel.getScaleFactor();
        this.radiusIncrease = 2;
        return this;
    }

    public float getRadiusIncrease() {
        return radiusIncrease;
    }

    public PointView setRadiusIncrease(float radiusIncrease) {
        this.radiusIncrease = radiusIncrease;
        return this;
    }

}
