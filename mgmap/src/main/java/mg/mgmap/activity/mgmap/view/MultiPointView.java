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
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;

import mg.mgmap.R;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.basic.MGLog;

/**
 */
public class MultiPointView extends MVLayer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    public static final int POINT_RADIUS = 4;
    private static final byte STROKE_MIN_ZOOM = 15;

    protected final GraphicFactory graphicFactory = AndroidGraphicFactory.INSTANCE;
    protected final boolean keepAligned = false;
    protected final Paint paintStroke;
    protected double strokeIncrease = 1.2;
    protected boolean showIntermediates = false;
    protected boolean showPointsOnly = false;
    protected int pointRadius = POINT_RADIUS;
    protected boolean enumeratePoints = false;
    private Paint paintText;
    private Paint paintTextBg;

    protected final MultiPointModel model;

    public MultiPointView(MultiPointModel model, Paint paintStroke) {
        this.model = model;
        this.paintStroke = paintStroke;
    }

    public MultiPointModel getModel(){
        return model;
    }

    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        try {
            if (getMapViewUtility().getTrackVisibility()){
                drawModel(model, boundingBox, zoomLevel, canvas, topLeftPoint);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
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

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (model){
            Iterator<PointModel> iterator = model.iterator();
            if (!iterator.hasNext()) {
                return;
            }

            Path path = this.graphicFactory.createPath();

            int cnt = 1;
            PointModel pm = iterator.next();
            int x = lon2canvasX(pm.getLon());
            int y = lat2canvasY(pm.getLat());
            path.moveTo(x, y);
            drawPoint(canvas, x,y, zoomLevel, cnt);

            while (iterator.hasNext()) {
                cnt++;
                pm = iterator.next();
                x = lon2canvasX(pm.getLon());
                y = lat2canvasY(pm.getLat());
                if ((this.paintStroke.getStrokeWidth() > 0) && (!showPointsOnly)){
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                }
                if ( (! iterator.hasNext()) || showIntermediates ){
                    drawPoint(canvas, x,y, zoomLevel, cnt);
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
    }

    private void drawPoint(Canvas canvas, int x, int y, byte zoomLevel, int cnt){
        if (pointRadius > 0){
            if (enumeratePoints){
                if (zoomLevel >= 12) {
                    canvas.drawCircle(x, y, (int)(displayModel.getScaleFactor() * pointRadius* getScale(zoomLevel)), enumeratePoints?CC.getFillPaint(R.color.CC_WHITE):this.paintStroke);
                    drawEnumeration(canvas, x, y, zoomLevel, cnt);
                }
            } else {
                canvas.drawCircle(x, y, (int)(displayModel.getScaleFactor() * pointRadius* getScale(zoomLevel)), enumeratePoints?CC.getFillPaint(R.color.CC_WHITE):this.paintStroke);
            }
        }
    }
    private void drawEnumeration(Canvas canvas, int x, int y, byte zoomLevel, int enumeration){
        if (paintText == null){
            paintText = CC.getFillPaint(R.color.CC_WHITE);
            paintText.setColor(paintStroke.getColor());
            paintText.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
            paintTextBg = CC.getStrokePaint(R.color.CC_WHITE, 10);
            paintTextBg.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
            int textSize = (int)(displayModel.getScaleFactor() * pointRadius* getScale(zoomLevel) *5);
            paintText.setTextSize(textSize);
            paintTextBg.setTextSize(textSize);
        }
        if (pointRadius > 0){
            int diff = (int)(0.8 * displayModel.getScaleFactor() * pointRadius* getScale(zoomLevel));
            canvas.drawText(""+enumeration, x+diff, y-diff, paintTextBg);
            canvas.drawText(""+enumeration, x+diff, y-diff, paintText);
        }
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

    public void setShowPointsOnly(boolean showPointsOnly) {
        this.showPointsOnly = showPointsOnly;
    }

    public void setPointRadius(int pointRadius) {
        this.pointRadius = pointRadius;
    }

    public void setEnumeratePoints(boolean enumeratePoints) {
        this.enumeratePoints = enumeratePoints;
    }

    protected float getScale(byte zoomLevel){
        return (float) Math.pow(this.strokeIncrease, Math.max(zoomLevel - STROKE_MIN_ZOOM, 0));
    }
}
