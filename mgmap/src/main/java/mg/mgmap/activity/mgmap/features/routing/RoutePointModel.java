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

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;

/** A model point of the route. Most important property is the reference to the corresponding
 * MarkerTrackLogPoint (PointModel). Additionally it keeps a reference to a MultiPointModel
 * that represents the route between the previous MarkerTrackLogPoint and this one. This is not
 * just the list of GNode instances - instead it is a list of ExtendedPointModelImpl instances with generic
 * type RoutingHint. These points contain already the routing hints as an extent object
 * and they also contain the relative amount of time from the beginning of this route. <br>
 * Finally it contains references to the best approach of this MarkerTrackLogPoint.
 * */
public class RoutePointModel {

    MultiPointModelImpl currentMPM = null;
    MultiPointModelImpl newMPM = null;
    final PointModel mtlp;
    double currentDistance = 0;

    ApproachModel selectedApproach;
    boolean aborted; // last route calculation aborted for this RPM

    public RoutePointModel(PointModel pointModel){
        this.mtlp = pointModel;
        resetApproaches();
    }

    ApproachModel getApproach(){
        return selectedApproach;
    }

    PointModel getApproachNode(){
        return (selectedApproach == null)?null:selectedApproach.getApproachNode();
    }


    public boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2){
        if (selectedApproach == null) return false;
        return selectedApproach.verifyApproach(node1,approachNode,node2);
    }

    public PointModel getMtlp() {
        return mtlp;
    }


    public void resetApproaches(){
        selectedApproach = null;
    }

    void setApproach(ApproachModel approachModel){
        selectedApproach = approachModel;
    }

}
