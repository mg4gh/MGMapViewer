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
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import mg.mgmap.generic.model.BBox;

public class BoxView extends MVLayer {

    private static final GraphicFactory graphicFactory = AndroidGraphicFactory.INSTANCE;

    private BBox model;
    private final Paint paintStroke;

    public BoxView(BBox model, Paint paintStroke){
        this.model = model;
        this.paintStroke = paintStroke;
    }

    @Override
    public synchronized void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (this.paintStroke == null){
            return;
        }

        if (! model.intersects(boundingBox)){
            return;
        }

        int minX = lon2canvasX(model.minLongitude);
        int maxX = lon2canvasX(model.maxLongitude);
        int minY = lat2canvasY(model.minLatitude);
        int maxY = lat2canvasY(model.maxLatitude);


        Path path = BoxView.graphicFactory.createPath();
        path.moveTo(minX, minY);
        path.lineTo(minX, maxY);
        path.lineTo(maxX, maxY);
        path.lineTo(maxX, minY);
        path.lineTo(minX, minY);

        this.paintStroke.setBitmapShaderShift(topLeftPoint);
        canvas.drawPath(path, this.paintStroke);
    }

    public BBox getModel() {
        return model;
    }

    public void setModel(BBox model) {
        this.model = model;
    }
}
