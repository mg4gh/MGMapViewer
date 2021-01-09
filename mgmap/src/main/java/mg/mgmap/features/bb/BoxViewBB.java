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
package mg.mgmap.features.bb;

import org.mapsforge.core.graphics.Paint;

import mg.mgmap.R;
import mg.mgmap.model.BBox;
import mg.mgmap.util.CC;
import mg.mgmap.view.BoxView;

public class BoxViewBB extends BoxView{

    private static final Paint PAINT_BOX_STROKE = CC.getStrokePaint(R.color.BLUE_A150, 4);

    public BoxViewBB(BBox model){
        super(model, PAINT_BOX_STROKE);
    }

}
