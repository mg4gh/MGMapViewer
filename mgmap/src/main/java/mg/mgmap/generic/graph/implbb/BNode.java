package mg.mgmap.generic.graph.implbb;

import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;

import java.util.Locale;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.PointNeighbour;
import mg.mgmap.generic.util.basic.LaLo;

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
        return LaLo.md2d(bGraphTile.nodes.getLatitude(nodeIdx));
    }

    @Override
    public double getLon() {
        return LaLo.md2d(bGraphTile.nodes.getLongitude(nodeIdx));
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

    @NonNull
    @Override
    public String toString() {
        if (getEle() == NO_ELE){
            return String.format(Locale.ENGLISH, "Lat=%2.6f, Lon=%2.6f",getLat(), getLon());
        } else{
            return String.format(Locale.ENGLISH, "Lat=%2.6f, Lon=%2.6f, Ele=%2.1fm",getLat(), getLon(), getEle());
        }
    }
}
