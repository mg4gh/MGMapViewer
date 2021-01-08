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
package mg.mgmap.model;

/** A model for a single point */
public interface PointModel {

    double NO_LAT_LONG = 200;
    float NO_ELE = -20000;
    float NO_PRES = 0;
    long NO_TIME = 0;

    double getLat();

    double getLon();

    /**
     * @return elevation for absolute elevation calculation (e.g. gps based or hgt based)
     */
    float getEleA();

    /**
     * @return elevation for delta elevation calculation (e.g. pressure based or hgt based)
     */
    float getEleD();

    long getTimestamp();

    long getLaLo();
}
