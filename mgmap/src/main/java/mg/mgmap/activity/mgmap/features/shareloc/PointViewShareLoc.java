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
package mg.mgmap.activity.mgmap.features.shareloc;

import org.mapsforge.core.graphics.Paint;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.view.PointView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.CC;

public class PointViewShareLoc extends PointView {

    public PointViewShareLoc(PointModel pointModel, int color){
        super(pointModel, CC.getStrokePaint4Color(color, 2), CC.getFillPaint4Color(color));
    }

}
