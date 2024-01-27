package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

public abstract class RoutingProfile {

    protected String id;
    protected CostCalculator costCalculator;

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

    public double getCost(WayAttributs wayAttributs, GNode node1, GNode node2){
        if ((wayAttributs!=null) && (wayAttributs.getDerivedData()==null)){
            wayAttributs.setDerivedData( getCostCalculator(costCalculator, wayAttributs));
        }
        double distance = PointModelUtil.distance(node1, node2);
        float verticalDistance = node2.getEleD() - node1.getEleD();
        return getCost(wayAttributs, distance, verticalDistance);
    }
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance){
        CostCalculator calculator = this.costCalculator;
        if ((wayAttributs != null) && (wayAttributs.getDerivedData() instanceof CostCalculator)){
            calculator = ((CostCalculator)wayAttributs.getDerivedData());
        }
        return calculator.calcCosts(distance, verticalDistance);
    }

    public double heuristic(PointModel node, PointModel target){
        double distance = PointModelUtil.distance(node, target);
        float verticalDistance = target.getEleD() - node.getEleD();
        return heuristic(distance, verticalDistance);
    }
    protected double heuristic(double distance, float verticalDistance){
        return costCalculator.heuristic(distance,verticalDistance);
    }



    abstract public int getIconIdActive();
    abstract protected int getIconIdInactive();


    void initETV(ExtendedTextView etv, Pref<String> prefCurrentRoutingProfileId){
        Pref<Boolean> rpState = new Pref<>(id.equals(prefCurrentRoutingProfileId.getValue()));
        prefCurrentRoutingProfileId.addObserver(evt -> rpState.setValue( id.equals(prefCurrentRoutingProfileId.getValue()) ));
        etv.setData(rpState, getIconIdInactive(), getIconIdActive());
        etv.setOnClickListener(v -> prefCurrentRoutingProfileId.setValue(id));
        etv.setName(prefCurrentRoutingProfileId.getKey());
    }
}
