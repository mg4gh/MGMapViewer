package mg.mgmap.application.util;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public class CostProvider {
    private MGMapApplication application = null;
    private Way way = null;
    private double cost;
    private float eleCost;
    public CostProvider(MGMapApplication application ) {this.application = application; }
    public void setWay(Way way) {this.way = way;}
    public void calcCost(PointModel node1, PointModel node2) {
        cost = PointModelUtil.distance(node1, node2) ;
        eleCost = + PointModelUtil.eleDiff(node1, node2) * 20;
    }
    public double getCost12() {
        if ( eleCost > 0 ) return cost + eleCost;
        else return cost;
    }

    public double getCost21() {
        if ( eleCost < 0 ) return cost - eleCost;
        else return cost;
    }

}
