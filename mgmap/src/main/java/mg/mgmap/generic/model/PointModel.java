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
package mg.mgmap.generic.model;

import org.mapsforge.core.model.LatLong;

/** A model for a single point */
public interface PointModel {

    double NO_LAT_LONG = 200;
    float NO_ELE = -20000;
    float NO_ACC = -20002;
    float NO_PRES = 0;
    long NO_TIME = 946771200000L;

    double getLat();

    double getLon();

    /**
     * @return elevation for absolute elevation calculation (e.g. gps based or hgt based)
     * For elevation delta (gain/loss) use PointModelUtil.verticalDistance()
     */
    float getEle();

    float getEleAcc();

    long getTimestamp();

    long getLaLo();

    LatLong getLatLong();
}
