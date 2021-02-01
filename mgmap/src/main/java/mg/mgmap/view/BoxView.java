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
package mg.mgmap.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import mg.mgmap.model.BBox;

public class BoxView extends MVLayer {

    private BBox model;
    private Paint paintStroke;
    private Paint paintFill = null;
    private boolean keepAligned = true;

    private static GraphicFactory graphicFactory = AndroidGraphicFactory.INSTANCE;

    public BoxView(BBox model, Paint paintStroke){
        this.model = model;
        this.paintStroke = paintStroke;
    }
    public BoxView(BBox model, Paint paintStroke, Paint paintFill){
        this(model, paintStroke);
        this.paintFill = paintFill;
    }
    public BoxView(BBox model, Paint paintStroke, Paint paintFill, boolean keepAligned){
        this(model, paintStroke, paintFill);
        this.keepAligned = keepAligned;
    }


    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.paintStroke == null && this.paintFill == null){
            return;
        }

        if (! model.intersects(boundingBox)){
            return;
        }

        int minX = lon2x(model.minLongitude);
        int maxX = lon2x(model.maxLongitude);
        int minY = lat2y(model.minLatitude);
        int maxY = lat2y(model.maxLatitude);


        Path path = BoxView.graphicFactory.createPath();
        path.moveTo(minX, minY);
        path.lineTo(minX, maxY);
        path.lineTo(maxX, maxY);
        path.lineTo(maxX, minY);
        path.lineTo(minX, minY);

        if (this.paintStroke != null) {
            if (this.keepAligned) {
                this.paintStroke.setBitmapShaderShift(topLeftPoint);
            }
            canvas.drawPath(path, this.paintStroke);
        }
        if (this.paintFill != null) {
            if (this.keepAligned) {
                this.paintFill.setBitmapShaderShift(topLeftPoint);
            }
            canvas.drawPath(path, this.paintFill);
        }
    }


    public BBox getModel() {
        return model;
    }

    public void setModel(BBox model) {
        this.model = model;
    }

    public void setKeepAligned(boolean keepAligned) {
        this.keepAligned = keepAligned;
    }
}
