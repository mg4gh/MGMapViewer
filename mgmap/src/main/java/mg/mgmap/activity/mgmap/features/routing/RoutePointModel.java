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
import java.util.TreeSet;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;

/** A model point of the route. Most important property is the reference to the corresponding
 * MarkerTrackLogPoint (PointModel). Additionally it keeps a reference to a MultiPointModel
 * that represents the route between the previous MarkerTrackLogPoint and this one. This is not
 * just the list of GNode instances - instead it is a list of ExtendedPointModelImpl instances with generic
 * type RoutingHint. These points contain already the routing hints as an extent object
 * and they also contain the relative amount of time from the beginning of this route. <br>
 * Finally it contains references to the approaches of this MarkerTrackLogPoint.
 * */
public class RoutePointModel {

    MultiPointModelImpl currentMPM = null;
    MultiPointModelImpl newMPM = null;
    final PointModel mtlp;
    double currentDistance = 0;

    ArrayList<ApproachModel> approaches;
    ApproachModel selectedApproach;
    BBox approachBBox;
    boolean aborted;

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

    /** @noinspection RedundantIfStatement*/
    public boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2){
        if (selectedApproach == null) return false;
        if (selectedApproach.getApproachNode() != approachNode) return false;
        if ((node1 == selectedApproach.getNode1()) && (node2 == selectedApproach.getNode2())) return true;
        if ((node1 == selectedApproach.getNode2()) && (node2 == selectedApproach.getNode1())) return true;
        return false;
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
