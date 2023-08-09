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
package mg.mgmap.activity.mgmap.features.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;

/** A model point of the route. Most important property is the reference to the corresponding
 * MarkerTrackLogPoint (PointModel). Additionally it keeps a reference to a MultiPointModel
 * that represents the route between the previous MarkerTrackLogPoint and this one.
 * Finally it contains references to the approaches of this MarkerTrackLogPoint.
 * */
public class RoutePointModel {

    HashMap<PointModel, RoutingHint> routingHints = new HashMap<>();
    MultiPointModelImpl currentMPM = null;
    MultiPointModelImpl newMPM = null;
    PointModel mtlp;
    double currentDistance = 0;

    ArrayList<ApproachModel> approaches;
    ApproachModel selectedApproach;
    BBox approachBBox;

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

    public ArrayList<ApproachModel> getApproaches() {
        return approaches;
    }

    public void resetApproaches(){
        approaches = null;
        selectedApproach = null;
        approachBBox = null;
    }

    void setApproaches(TreeSet<ApproachModel> approaches){
        this.approaches = new ArrayList<>( approaches );
        approachBBox = new BBox();
        if (!approaches.isEmpty()){
            for (ApproachModel am : approaches){
                approachBBox.extend(am.getNode1()).extend(am.getNode2());
            }
            selectedApproach = approaches.first();
        }
    }

}
