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
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

import java.util.Iterator;

import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.model.PointModelUtil;

/** This view draws a track (segment) via its MultiPointModel. The special thing here is, that the
 * color depends on the gain/loss of the segment.
 * A smoothing function will be applied.
 */
public class MultiPointGLView extends MultiPointView {


    public MultiPointGLView(MultiPointModel model, Paint paintStroke) {
        super(model, paintStroke);
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

        {
            Path path = this.graphicFactory.createPath();
            PointModel pm1 = model.get(model.size() - 1), pm2;
            int x1 = lon2canvasX(pm1.getLon()), x2=x1, x3;
            int y1 = lat2canvasY(pm1.getLat()), y2=y1, y3;
            float z1 = pm1.getEleD(), z2;
            double d1=0, d2=d1, d3;
            double gl1=0, gl2=gl1, gl3;

            float scale = getScale(zoomLevel);
            float w0 = paintStroke.getStrokeWidth();

            // With a given i (Point pm1 with x1,y1,z1) we draw the line between index i-1 (pm2,x2,y2,z2) and
            // index i-2 (x3,y3,z3).
            // The interval between i and i-1 has the distance d1, and the gain/loss gl1,
            // the interval between i-1 and i-2 has the distance d2, and the gain/loss gl2,
            // the interval between i-2 and i-3 has the distance d3, and the gain/loss gl3,
            // For the segment to draw the glValue is calculated from these three segments,
            // the weight of the predecessor is 0.3*d3, current weight is 1*d2, the successor weight is 0.3*d1
            for (int i = model.size() - 2; i >= -1; i--) {
                pm2=pm1; pm1 = getPM(model, i);
                x3=x2; x2=x1; x1 = lon2canvasX(pm1.getLon());
                y3=y2; y2=y1; y1 = lat2canvasY(pm1.getLat());
                z2=z1; z1 = pm1.getEleD();
                d3=d2; d2=d1; d1=PointModelUtil.distance(pm2,pm1);
                gl3=gl2; gl2=gl1; gl1= (d1==0)?0:(z2-z1)/(d1 / 100.0);

                if (d2 > 0){
                    double f1=0.3, f2=1, f3=0.3;
                    double d = d1*f1 + d2*f2 + d3*f3;
                    double we1=d1*f1/d, we2=d2*f2/d, we3=d3*f3/d;
                    float glValue= (float)(gl1*we1 + gl2*we2 + gl3*we3);

                    path.clear();
                    path.moveTo(x3, y3);
                    path.lineTo(x2, y2);
                    // first draw with paintStroke (background)
                    paintStroke.setStrokeWidth(w0*scale);
                    canvas.drawPath(path, paintStroke);
                    // then draw with variable color (depending on gain/loss) 70% width
                    Paint paint = CC.getGlPaint( glValue);
                    float w1 = paint.getStrokeWidth();
                    paint.setStrokeWidth( w0 * 0.7f * scale);
                    canvas.drawPath(path, paint);
                    // finally draw thin line in the direction of path (last 20%)
                    path.clear();
                    path.moveTo((x2+4*x3)/5f, (y2+4*y3)/5f);
                    paintStroke.setStrokeWidth( w0 * 0.7f * scale / 3);
                    path.lineTo(x3,y3);
                    canvas.drawPath(path, paintStroke);

                    paint.setStrokeWidth(w1);
                }
            }
            this.paintStroke.setStrokeWidth(w0);
        }
    }

    PointModel getPM(MultiPointModel model, int i){
        if (i < 0){
            i=0;
        } else if (i>=model.size()){
            i=model.size()-1;
        }
        return model.get(i);
    }
}
