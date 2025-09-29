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

import mg.mgmap.generic.util.basic.LaLo;

/** A implementation of a point model with changeable model data */
public class WriteablePointModelImpl extends PointModelImpl implements WriteablePointModel {

    long timestamp = NO_TIME;

    public WriteablePointModelImpl(PointModel pm){
        this(pm.getLat(), pm.getLon(), pm.getEle(), pm.getEleAcc());
    }

    public WriteablePointModelImpl(double latitude, double longitude, float ele, float eleAcc) {
        super(latitude, longitude, ele, eleAcc);
    }

    public WriteablePointModelImpl(double latitude, double longitude) {
        super(latitude, longitude);
    }

    public WriteablePointModelImpl(){
        super( NO_LAT_LONG, NO_LAT_LONG);
    }

    @Override
    public void setLat(double latitude) {
        la = LaLo.d2md(latitude);
    }
    @Override
    public void setLa(int la) {
        this.la = la;
    }
    @Override
    public void setLon(double longitude) {
        lo = LaLo.d2md(longitude);
    }
    @Override
    public void setLo(int lo) {
        this.lo = lo;
    }

    @Override
    public void setEle(float elevation) {
        if (elevation != NO_ELE){
            ele = elevation;
        }
    }
    @Override
    public void setEleAcc(float eleAcc) {
        this.eleAcc = eleAcc;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
