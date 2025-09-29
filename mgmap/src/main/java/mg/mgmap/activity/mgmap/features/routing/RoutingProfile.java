package mg.mgmap.activity.mgmap.features.routing;

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

    public float getCost(WayAttributs wayAttributs, PointModel node, PointModel neighbourNode, boolean primaryDirection){
        return getCost(wayAttributs, (float)PointModelUtil.distance(node,neighbourNode), PointModelUtil.verticalDistance(node, neighbourNode), primaryDirection);
    }
    public float getCost(WayAttributs wayAttributs, float distance, float verticalDistance, boolean primaryDirection){
        CostCalculator calculator = this.costCalculator;
        if (wayAttributs != null){
            if (wayAttributs.getDerivedData()==null){
                wayAttributs.setDerivedData( getCostCalculator(costCalculator, wayAttributs));
            }
            if (wayAttributs.getDerivedData() instanceof CostCalculator wayCostCalculator){
                calculator = wayCostCalculator;
            }
        }
        return (float)calculator.calcCosts(distance, verticalDistance, primaryDirection);
    }

    public double heuristic(PointModel node1, PointModel node2){
        return heuristic(PointModelUtil.distance(node1, node2), PointModelUtil.verticalDistance(node1,node2));
    }
    public double heuristic(double distance, float verticalDistance){
        return costCalculator.heuristic(distance,verticalDistance);
    }

    public long getDuration(WayAttributs wayAttributs, PointModel node1, PointModel node2){
        return getDuration(wayAttributs, PointModelUtil.distance(node1, node2), PointModelUtil.verticalDistance(node1,node2));

    }
    protected long getDuration(WayAttributs wayAttributs, double distance, float verticalDistance){
        CostCalculator calculator = this.costCalculator;
        if ((wayAttributs != null) && (wayAttributs.getDerivedData() instanceof CostCalculator wayCostCalculator)){
            calculator = wayCostCalculator;
        }
        return calculator.getDuration(distance,verticalDistance);
    }



    abstract public int getIconIdActive();
    abstract protected int getIconIdInactive();

    protected int getIconIdCalculating(){
        return getIconIdActive();
    }

}
