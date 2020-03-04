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
package mg.mapviewer.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.Iterator;

import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;

/**
 */
public class MultiPointView extends MVLayer {

    private static final byte STROKE_MIN_ZOOM = 15;

    protected final GraphicFactory graphicFactory = AndroidGraphicFactory.INSTANCE;
    protected boolean keepAligned = false;
    protected Paint paintStroke;
    protected double strokeIncrease = 1.2;
    protected boolean showIntermediates = false;
    protected int pointRadius = 4;

    protected volatile MultiPointModel model;

    public MultiPointView(MultiPointModel model, Paint paintStroke) {
        this.model = model;
        this.paintStroke = paintStroke;
    }

    public MultiPointModel getModel(){
        return model;
    }


//    public synchronized boolean contains(Point tapXY, MapViewProjection mapViewProjection) {
//        // Touch min 20 px at baseline mdpi (160dpi)
//        double distance = Math.max(20 / 2 * this.displayModel.getScaleFactor(),
//                this.paintStroke.getStrokeWidth() / 2);
//        Point point2 = null;
//        for (int i = 0; i < this.model.size() - 1; i++) {
//            LatLong latLong = new LatLong(this.model.get(i).getLat(),this.model.get(i).getLon());
//            Point point1 = i == 0 ? mapViewProjection.toPixels(latLong) : point2;
//            LatLong latLong2 = new LatLong(this.model.get(i+1).getLat(),this.model.get(i+1).getLon());
//            point2 = mapViewProjection.toPixels(latLong2);
//            if (LatLongUtils.distanceSegmentPoint(point1.x, point1.y, point2.x, point2.y, tapXY.x, tapXY.y) <= distance) {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        drawModel(model, boundingBox, zoomLevel, canvas, topLeftPoint);
    }

    protected void drawModel(MultiPointModel model, BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint){
        if ((model == null) ||(model.size() <= 0) || this.paintStroke == null) {
            return;
        }

        if ( ! model.getBBox().intersects( boundingBox )) {
            return;
        }
        if ( model.getBBox().contains(PointModel.NO_LAT_LONG, PointModel.NO_LAT_LONG)) {
            return;
        }

        Iterator<PointModel> iterator = model.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        Path path = this.graphicFactory.createPath();

        PointModel pm = iterator.next();
        int x = lon2x(pm.getLon());
        int y = lat2y(pm.getLat());
        path.moveTo(x, y);
        canvas.drawCircle(x, y, pointRadius, this.paintStroke);

        while (iterator.hasNext()) {
            pm = iterator.next();
            x = lon2x(pm.getLon());
            y = lat2y(pm.getLat());
            path.lineTo(x, y);
            if ( (! iterator.hasNext()) || showIntermediates ){
                if (pointRadius > 0){
                    canvas.drawCircle(x, y, pointRadius, this.paintStroke);
                }
            }
        }

        if (this.keepAligned) {
            this.paintStroke.setBitmapShaderShift(topLeftPoint);
        }
        float strokeWidth = this.paintStroke.getStrokeWidth();
        if (this.strokeIncrease > 1) {
            this.paintStroke.setStrokeWidth(strokeWidth * getScale(zoomLevel));
        }
        canvas.drawPath(path, this.paintStroke);
        this.paintStroke.setStrokeWidth(strokeWidth);
    }

    /**
     * @return the {@code Paint} used to stroke this polyline (may be null).
     */
    public synchronized Paint getPaintStroke() {
        return this.paintStroke;
    }

    /**
     * @return the base to scale polyline stroke per zoom (default 1 not scale).
     */
    public synchronized double getStrokeIncrease() {
        return strokeIncrease;
    }

    /**
     * @return true if it keeps the bitmap aligned with the map, to avoid a
     * moving effect of a bitmap shader, false otherwise.
     */
    public boolean isKeepAligned() {
        return keepAligned;
    }

    public void setKeepAligned(boolean keepAligned) {
        this.keepAligned = keepAligned;
    }

    /**
     * @param paintStroke the new {@code Paint} used to stroke this polyline (may be null).
     */
    public synchronized void setPaintStroke(Paint paintStroke) {
        this.paintStroke = paintStroke;
    }


    /**
     * @param strokeIncrease the base to scale polyline stroke per zoom (default 1 not scale).
     */
    public synchronized void setStrokeIncrease(double strokeIncrease) {
        this.strokeIncrease = strokeIncrease;
    }

    public void setShowIntermediates(boolean showIntermediates) {
        this.showIntermediates = showIntermediates;
    }

    public void setPointRadius(int pointRadius) {
        this.pointRadius = pointRadius;
    }



    protected float getScale(byte zoomLevel){
        return (float) Math.pow(this.strokeIncrease, Math.max(zoomLevel - STROKE_MIN_ZOOM, 0));
    }
}
