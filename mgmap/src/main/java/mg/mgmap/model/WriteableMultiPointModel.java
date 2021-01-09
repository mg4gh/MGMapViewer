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

/** A writeable model for multiple points. Writeable in that sense means to add, to insert and to remove points */
public interface WriteableMultiPointModel extends MultiPointModel {

    WriteableMultiPointModel addPoint(PointModel pointModel);

    WriteableMultiPointModel addPoint(int idx, PointModel pointModel);

    boolean removePoint(PointModel pointModel);

    PointModel removePoint(int idx);
}
