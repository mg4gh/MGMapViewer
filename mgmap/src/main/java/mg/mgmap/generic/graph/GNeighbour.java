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
package mg.mgmap.generic.graph;

/**
 * This class is the basis for the neighbour relationship in a graph.
 */

public class GNeighbour{

    private final GNode neighbourNode;
    private final double cost;
    private GNeighbour nextNeighbour = null;

    public GNeighbour(GNode neighbourNode, double cost){
        this.neighbourNode = neighbourNode;
        this.cost = cost;
    }


    public GNode getNeighbourNode() {
        return neighbourNode;
    }

    public double getCost() {
        return cost;
    }

    public GNeighbour getNextNeighbour() {
        return nextNeighbour;
    }

    public void setNextNeighbour(GNeighbour nextNeighbour) {
        this.nextNeighbour = nextNeighbour;
    }

}
