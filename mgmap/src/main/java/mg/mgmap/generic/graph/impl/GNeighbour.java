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
package mg.mgmap.generic.graph.impl;

import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointNeighbour;

/**
 * This class is the basis for the neighbour relationship in a graph.
 */

public class GNeighbour implements PointNeighbour {

    private final GNode neighbourNode;
    private float cost;
    private float distance = -1;
    private GNeighbour nextNeighbour = null;
    private WayAttributs wayAttributs = null;
    private boolean primaryDirection = true;
    private GNeighbour reverse = null;

    public GNeighbour(GNode neighbourNode, float cost){
        this.neighbourNode = neighbourNode;
        this.cost = cost;
    }
    public GNeighbour(GNode neighbourNode, WayAttributs wayAttributs){
        this.neighbourNode = neighbourNode;
        this.wayAttributs = wayAttributs;
        resetCost();
    }

    @Override
    public PointModel getPoint() {
        return getNeighbourNode();
    }

    public GNode getNeighbourNode() {
        return neighbourNode;
    }

    public float getCost() {
        return cost;
    }
    void resetCost(){
        cost = -1;
    }
    public void setCost(float cost){
        this.cost = cost;
    }

    public float getDistance() {
        return distance;
    }
    public void setDistance(float distance) {
        this.distance = distance;
    }

    public GNeighbour getNextNeighbour() {
        return nextNeighbour;
    }

    public void setNextNeighbour(GNeighbour nextNeighbour) {
        this.nextNeighbour = nextNeighbour;
    }

    public boolean isPrimaryDirection() {
        return primaryDirection;
    }

    public GNeighbour setPrimaryDirection(boolean primaryDirection) {
        this.primaryDirection = primaryDirection;
        return this;
    }

    public WayAttributs getWayAttributs() {
        return wayAttributs;
    }

    public GNeighbour getReverse() {
        return reverse;
    }

    public void setReverse(GNeighbour reverse) {
        this.reverse = reverse;
    }
}
