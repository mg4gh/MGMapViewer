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
package mg.mapviewer.features.marker;

import org.mapsforge.core.graphics.Paint;

import mg.mapviewer.R;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.util.CC;
import mg.mapviewer.view.MultiPointView;

public class MarkerTrackView extends MultiPointView {

    private static final Paint PAINT_MARKER_STROKE_DASHED = CC.getStrokePaint(R.color.RED_A150, 3);

    static{
//        PAINT_MARKER_STROKE_DASHED.setDashPathEffect(new float[]{40.0f,20.0f});
    }

    public MarkerTrackView(MultiPointModel model) {
        super(model, PAINT_MARKER_STROKE_DASHED);
        this.setPointRadius(10);
        this.setShowIntermediates(true);
    }

}
