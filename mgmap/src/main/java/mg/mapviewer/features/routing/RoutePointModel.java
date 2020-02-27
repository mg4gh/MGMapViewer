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
package mg.mapviewer.features.routing;

import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

import mg.mapviewer.graph.GNode;
import mg.mapviewer.graph.ApproachModel;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;

/** A model point of the route. Most important property is the reference to the corresponding MarkerTrackLogPoint (PointModel).
 * Additionally it keeps a reference to a MultiPointModel that represents the route between the previous MarkerTrackLogPoint and this one.
 * Finally it contains references to the approaches of this MarkerTrackLogPoint.
 * */
public class RoutePointModel implements Observer {

    MultiPointModel currentMPM = null;
    MultiPointModel newMPM = null;
    PointModel mtlp;

    TreeSet<ApproachModel> approaches;
    ApproachModel selectedApproach;

    public RoutePointModel(PointModel pointModel){
        this.mtlp = pointModel;
        resetApproaches();
    }

    ApproachModel getApproach(){
        return selectedApproach;
    }

    GNode getApproachNode(){
        return (selectedApproach == null)?null:selectedApproach.getApproachNode();
    }

    public PointModel getMtlp() {
        return mtlp;
    }

    public TreeSet<ApproachModel> getApproaches() {
        return approaches;
    }

    // called if GGraphTile is removed from the cache - so approaches are no longer valid
    @Override
    public void update(Observable o, Object arg) {
        resetApproaches();
    }

    public void resetApproaches(){
        approaches = null;
        selectedApproach = null;

    }
}
