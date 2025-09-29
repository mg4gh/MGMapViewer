package mg.mgmap.generic.graph.impl2;

import org.mapsforge.core.model.LatLong;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.LaLo;

public class GIntermediateNode implements PointModel {

    GNode node;
    GNeighbour neighbour;
    int pIdx; // to intermediate neighbour

    public GIntermediateNode(GNode node, GNeighbour neighbour, int pIdx) {
        this.node = node;
        this.neighbour = neighbour;
        this.pIdx = pIdx;
    }

    public int getPIdx() {
        return pIdx;
    }

    @Override
    public double getLat() {
        return LaLo.md2d(neighbour.getIntermediatesPoints()[pIdx*3]);
    }

    @Override
    public double getLon() {
        return LaLo.md2d(neighbour.getIntermediatesPoints()[pIdx*3+1]);
    }

    @Override
    public float getEle() {
        return neighbour.getIntermediatesPoints()[pIdx*3+2] / PointModelUtil.ELE_FACTOR;
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
        return (((long)neighbour.getIntermediatesPoints()[pIdx*3])<<32) +neighbour.getIntermediatesPoints()[pIdx*3+1];
    }

    @Override
    public LatLong getLatLong() {
        return new LatLong(getLat(), getLon());
    }

    @Override
    public int compareTo(PointModel o) {
        return PointModelUtil.compareTo(this, o);
    }
}
