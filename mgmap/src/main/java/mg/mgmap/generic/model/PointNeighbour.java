package mg.mgmap.generic.model;

import mg.mgmap.generic.graph.WayAttributs;

public interface PointNeighbour {

    PointModel getPoint();
    PointNeighbour getNextNeighbour();
    WayAttributs getWayAttributs();
    float getDistance();
}
