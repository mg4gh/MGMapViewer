package mg.mgmap.generic.graph.implbb;

import org.mapsforge.core.model.LatLong;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.PointNeighbour;

public class BNode implements PointModel {

    BGraphTile bGraphTile;
    short nodeIdx;

    public BNode(){}

    public BNode(BGraphTile bGraphTile, short nodeIdx) {
        this.bGraphTile = bGraphTile;
        this.nodeIdx = nodeIdx;
    }

    public void setBGraphTile(BGraphTile bGraphTile) {
        this.bGraphTile = bGraphTile;
    }

    public void setNodeIdx(short nodeIdx) {
        this.nodeIdx = nodeIdx;
    }

    @Override
    public double getLat() {
        return bGraphTile.nodes.getLatitude(nodeIdx);
    }

    @Override
    public double getLon() {
        return bGraphTile.nodes.getLongitude(nodeIdx);
    }

    @Override
    public float getEle() {
        return bGraphTile.nodes.getEle(nodeIdx);
    }

    @Override
    public float getEleAcc() {
        return 0;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public long getLaLo() {
        return 0;
    }

    @Override
    public LatLong getLatLong() {
        return null;
    }

    @Override
    public int compareTo(PointModel o) {
        return PointModelUtil.compareTo(getLat(), getLon(), o.getLat(), o.getLon());
    }
}
