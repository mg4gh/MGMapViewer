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

import java.util.ArrayList;
import java.util.Iterator;

/** An implementation of a writeable model for multiple points. */
public class MultiPointModelImpl implements WriteableMultiPointModel{

    ArrayList<PointModel> points = new ArrayList<>();
    BBox bBox = new BBox();
    boolean route = false;

    public MultiPointModelImpl(){}

    public MultiPointModelImpl(MultiPointModel multiPointModel){
        for (int i = 0; i<multiPointModel.size(); i++){
            addPoint( multiPointModel.get(i) );
        }
    }

    @Override
    public MultiPointModelImpl addPoint(PointModel pointModel){
        return addPoint(size(), pointModel);
    }
    @Override
    public MultiPointModelImpl addPoint(int idx, PointModel pointModel) {
        points.add(idx, pointModel);
        bBox.extend(pointModel);
        return this;
    }

    @Override
    public boolean removePoint(PointModel pm){
        int idx = points.indexOf(pm);
        if (idx == -1) return false;
        removePoint(idx);
        return true;
    }
    @Override
    public PointModel removePoint(int idx) {
        PointModel res = points.remove(idx);
        recalcBBox();
        return res;
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public PointModel get(int i) {
        return points.get(i);
    }

    @NonNull
    @Override
    public Iterator<PointModel> iterator() {
        return points.iterator();
    }

    @Override
    public BBox getBBox() {
        return bBox;
    }

    private void recalcBBox(){
        bBox.clear().extend(points);
    }

    public boolean isRoute() {
        return route;
    }

    public void setRoute(boolean route) {
        this.route = route;
    }
}
