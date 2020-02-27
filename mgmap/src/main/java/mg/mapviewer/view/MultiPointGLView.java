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
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import java.util.Iterator;

import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.PointModelUtil;

/**
 */
public class MultiPointGLView extends MultiPointView {


    public MultiPointGLView(MultiPointModel model, Paint paintStroke) {
        super(model, paintStroke);
    }

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

        super.drawModel(model, boundingBox, zoomLevel, canvas, topLeftPoint);

        {
            Path path = this.graphicFactory.createPath();
            PointModel pm = model.get(model.size() - 1);
            int x = lon2x(pm.getLon());
            int y = lat2y(pm.getLat());
            float z = pm.getEleD();
            path.moveTo(x, y);

            float scale = getScale(zoomLevel);
            float w0 = this.paintStroke.getStrokeWidth();
            this.paintStroke.setStrokeWidth(w0*scale);


            for (int i = model.size() - 2; i >= 0; i--) {
                PointModel lpm = pm;
                int lx = x;
                int ly = y;
                float lz = z;

                pm = model.get(i);
                x = lon2x(pm.getLon());
                y = lat2y(pm.getLat());
                z = pm.getEleD();
                path.lineTo(x, y);
                double distance = PointModelUtil.distance(lpm, pm);
                double glValue = (lz - z) / (distance / 100.0);

                Paint paint = CC.getGlPaint((float) glValue);
                float w1 = paint.getStrokeWidth();
                paint.setStrokeWidth( w1 * scale);
                canvas.drawPath(path, paint);

                int x2 = (x+4*lx)/5;
                int y2 = (y+4*ly)/5;
                path.clear();
                path.moveTo(x2,y2);
                paintStroke.setStrokeWidth( w1 * scale / 3);
                path.lineTo(lx,ly);
                canvas.drawPath(path, paintStroke);

                paint.setStrokeWidth(w1);



                if (getMapViewUtility().getZoomLevel() >= 15) {
                    paintStroke.setStyle(Style.STROKE);
                    paintStroke.setStrokeWidth(w1*scale);
                    canvas.drawCircle(x, y, (int)(2*scale), this.paintStroke);
                }
                path.clear();
                path.moveTo(x, y);
            }
            this.paintStroke.setStrokeWidth(w0);

        }
    }


}
