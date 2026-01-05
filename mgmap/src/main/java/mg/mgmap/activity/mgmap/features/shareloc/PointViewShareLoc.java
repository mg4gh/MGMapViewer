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

import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mgmap.activity.mgmap.view.PointView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.VUtil;

public class PointViewShareLoc extends PointView {

    static SimpleDateFormat sdf = new SimpleDateFormat(FSShareLoc.DATE_FORMAT, Locale.ENGLISH);

    public PointViewShareLoc(PointModel pointModel, SharePerson person, Pref<Boolean> showLocationText){


        super(pointModel, CC.getStrokePaint4Color(person.color, VUtil.dp(2f)), CC.getFillPaint4Color(setAlpha(person.color, getAlpha(person.color)/2)));
        setRadius(VUtil.dp(3));
        if (showLocationText.getValue()) {
            setText(person.email + "\n" + sdf.format(pointModel.getTimestamp()));
        } else {
            setText(null);
        }

    }

    private static int getAlpha(int color){
        return (0xFF & color>>24);
    }

    private static int setAlpha(int color, int alpha){
        return ((color & 0xFFFFFF) | (alpha <<24));
    }

    @Override
    protected int getTextSize(int radiusInPixel) {
        return (int)( super.getTextSize(radiusInPixel) * 1.8f);
    }
}
