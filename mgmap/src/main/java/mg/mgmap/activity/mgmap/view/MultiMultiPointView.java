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

import java.util.ArrayList;

import mg.mgmap.generic.model.MultiPointModel;

public class MultiMultiPointView extends MultiPointView {

    final ArrayList<MultiPointModel> mpms;

    public MultiMultiPointView(ArrayList<MultiPointModel> mpms, Paint paint){
        super(null, paint);
        this.mpms = mpms;
    }

    @Override
    public void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        for (int i=0; i<mpms.size(); i++){
            drawModel(mpms.get(i), boundingBox, zoomLevel, canvas, topLeftPoint);
        }
    }

}
