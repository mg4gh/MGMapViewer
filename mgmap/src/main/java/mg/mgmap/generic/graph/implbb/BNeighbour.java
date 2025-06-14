package mg.mgmap.generic.graph.implbb;

import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointNeighbour;

public class BNeighbour implements PointNeighbour {

    BGraphTile bGraphTile;
    short neighbourIdx;

    public void setBGraphTile(BGraphTile bGraphTile) {
        this.bGraphTile = bGraphTile;
    }

    public void setNeighbourIdx(short neighbourIdx) {
        this.neighbourIdx = neighbourIdx;
    }

    public BNeighbour(){}

    public BNeighbour(BGraphTile bGraphTile, short neighbourIdx){
        this.bGraphTile = bGraphTile;
        this.neighbourIdx = neighbourIdx;
    }


    @Override
    public PointModel getPoint() {
        return new BNode(bGraphTile, bGraphTile.neighbours.getNeighbourPoint(neighbourIdx));
    }

    @Override
    public PointNeighbour getNextNeighbour() {
        return new BNeighbour(bGraphTile, bGraphTile.neighbours.getNextNeighbour(neighbourIdx));

    }

    @Override
    public WayAttributs getWayAttributs() {
        return bGraphTile.wayAttributes[ bGraphTile.neighbours.getWayAttributes(neighbourIdx) ];
    }

    @Override
    public double getDistance() {
        return bGraphTile.neighbours.getDistance(neighbourIdx);
    }
}
