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
 * Overlay neighbours are used to model extension in a graph keeping the original graph untouched. This is useful
 * for the multi tile graph as well as for the modelling of approaches, which split a node neighbour relationship
 * into two parts.
 */

public class GOverlayNeighbour {

    GNode node;
    final GNeighbour neighbour;
    final GNeighbour nextNeighbour;

    public GOverlayNeighbour(GNode node, GNeighbour neighbour, GNeighbour nextNeighbour) {
        this.node = node;
        this.neighbour = neighbour;
        this.nextNeighbour = nextNeighbour;
    }

    public GNode getNode() {
        return node;
    }

    public void setNode(GNode node) {
        this.node = node;
    }

    public GNeighbour getNeighbour() {
        return neighbour;
    }

}
