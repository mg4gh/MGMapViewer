package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public abstract class RoutingProfile {

    protected final String id;
    protected final CostCalculator costCalculator;

    public RoutingProfile(CostCalculator costCalculator){
        this.costCalculator = costCalculator;
        id = constructId(this.getClass());
    }

    public static String constructId(Class<?> clazz){
        return "RP_"+clazz.getSimpleName();
    }

    public String getId(){
        return id;
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs){
        return profileCalculator; // default is no way specific calculator
    }

    public double getCost(PointModel node1, GNeighbour neighbour, PointModel node2){
        WayAttributs wayAttributs = neighbour.getWayAttributs();
        if ((wayAttributs!=null) && (wayAttributs.getDerivedData()==null)){
            wayAttributs.setDerivedData( getCostCalculator(costCalculator, wayAttributs));
        }
        return getCost(wayAttributs, neighbour.getDistance(), PointModelUtil.verticalDistance(node1,node2), neighbour.isPrimaryDirection());

    }
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance, boolean primaryDirection){
        CostCalculator calculator = this.costCalculator;
        if ((wayAttributs != null) && (wayAttributs.getDerivedData() instanceof CostCalculator)){
            calculator = ((CostCalculator)wayAttributs.getDerivedData());
        }
        return calculator.calcCosts(distance, verticalDistance, primaryDirection);
    }

    public double heuristic(PointModel node1, PointModel node2){
        return heuristic(PointModelUtil.distance(node1, node2), PointModelUtil.verticalDistance(node1,node2));
    }
    protected double heuristic(double distance, float verticalDistance){
        return costCalculator.heuristic(distance,verticalDistance);
    }

    public long getDuration(PointModel node1, GNeighbour neighbour, PointModel node2){
        if (neighbour != null){
            return getDuration(neighbour.getWayAttributs(), neighbour.getDistance(), PointModelUtil.verticalDistance(node1,node2));
        } else {
            return getDuration(null, PointModelUtil.distance(node1, node2), PointModelUtil.verticalDistance(node1,node2));
        }
    }
    protected long getDuration(WayAttributs wayAttributs, double distance, float verticalDistance){
        CostCalculator calculator = this.costCalculator;
        if ((wayAttributs != null) && (wayAttributs.getDerivedData() instanceof CostCalculator)){
            calculator = ((CostCalculator)wayAttributs.getDerivedData());
        }
        return calculator.getDuration(distance,verticalDistance);
    }



    abstract public int getIconIdActive();
    abstract protected int getIconIdInactive();

    protected int getIconIdCalculating(){
        return getIconIdActive();
    }

}
