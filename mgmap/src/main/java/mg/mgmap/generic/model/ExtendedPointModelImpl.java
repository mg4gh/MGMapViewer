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

public class ExtendedPointModelImpl<T> extends WriteablePointModelImpl implements ExtendedPointModel<T>{

    private final T extent;

    public static <T> ExtendedPointModelImpl<T> createFromLaLo(int la, int lo, int el, T extent){
        ExtendedPointModelImpl<T> epmi = new ExtendedPointModelImpl<>(extent);
        epmi.la = la;
        epmi.lo = lo;
        epmi.ele = el/PointModelUtil.ELE_FACTOR;
        return epmi;
    }



    private ExtendedPointModelImpl(T extent) {
        this.extent = extent;
    }

    public ExtendedPointModelImpl(PointModel pm, T extent){
        this(pm.getLat(), pm.getLon(), pm.getEle(), pm.getEleAcc(), extent);
    }

    public ExtendedPointModelImpl(double latitude, double longitude, float ele, float eleAcc, T extent){
        super(latitude, longitude, ele, eleAcc);
        this.extent = extent;
    }

    @Override
    public T getExtent() {
        return extent;
    }
}
