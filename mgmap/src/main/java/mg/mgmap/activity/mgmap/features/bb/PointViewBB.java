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
package mg.mgmap.activity.mgmap.features.bb;

import org.mapsforge.core.graphics.Paint;

import mg.mgmap.R;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.activity.mgmap.view.PointView;

public class PointViewBB extends PointView {

    private static final Paint PAINT_MARKER_FILL = CC.getFillPaint(R.color.BLUE100_A100);
    private static final Paint PAINT_MARKER_STROKE = CC.getStrokePaint(R.color.BLUE_A150, 2);

    public PointViewBB(PointModel pointModel){
        super(pointModel, PAINT_MARKER_STROKE, PAINT_MARKER_FILL);
    }

}
