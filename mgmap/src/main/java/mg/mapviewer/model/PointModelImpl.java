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
package mg.mapviewer.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mapsforge.core.model.LatLong;

import java.nio.ByteBuffer;
import java.util.Locale;

import mg.mapviewer.util.LaLo;
import mg.mapviewer.util.PointModelUtil;

/** A simple implementation for a single point */
public class PointModelImpl implements PointModel, Comparable<PointModel>{

    int la;
    int lo;
    float ele = NO_ELE;

    public static PointModelImpl createFromLaLo(int la, int lo){
        PointModelImpl pmi = new PointModelImpl();
        pmi.la = la;
        pmi.lo = lo;
        return pmi;
    }

    public PointModelImpl(PointModel pm){
        this(pm.getLat(), pm.getLon(), pm.getEleA());
    }

    public PointModelImpl(double latitude, double longitude, float ele){
        this(latitude, longitude);
        this.ele = ele;
    }

    public PointModelImpl(double latitude, double longitude){
        this.la = LaLo.d2md(latitude);
        this.lo = LaLo.d2md(longitude);;
    }

    public PointModelImpl(LatLong latLong){
        this(latLong.latitude, latLong.longitude);
    }

    public PointModelImpl(){}

    @Override
    public double getLat() {
        return LaLo.md2d(la);
    }
    @Override
    public double getLon() {
        return LaLo.md2d(lo);
    }

    @Override
    public float getEleA(){
        return ele;
    }

    @Override
    public float getEleD() {
        return ele;
    }

    @Override
    public long getTimestamp() {
        return PointModel.NO_TIME;
    }

    @NonNull
    @Override
    public String toString() {
        if (ele == NO_ELE){
            return String.format(Locale.GERMAN, "Lat=%2.6f, Lon=%2.6f",getLat(), getLon());
        } else{
            return String.format(Locale.GERMAN, "Lat=%2.6f, Lon=%2.6f, ELe=%2.1fm",getLat(), getLon(), ele);

        }
    }

    public void toBuf(ByteBuffer buf){
        buf.putInt(la);
        buf.putInt(lo);
        buf.putFloat(ele);
    }

    public void fromBuf(ByteBuffer buf){
        la = buf.getInt();
        lo = buf.getInt();
        ele = buf.getFloat();
    }

    @Override
    public int compareTo(@NonNull PointModel o) {
        return PointModelUtil.compareTo(this, o);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PointModelImpl) {
            PointModelImpl oo = (PointModelImpl) obj;
            return ((la == oo.la) && (lo == oo.lo) && (ele == oo.ele));
        }
        return false;
    }

    public int laMdDiff(PointModelImpl otherPointModelImpl){
        return Math.abs(this.la - otherPointModelImpl.la);
    }
    public int loMdDiff(PointModelImpl otherPointModelImpl){
        return Math.abs(this.lo - otherPointModelImpl.lo);
    }
}
