package mg.mgmap.activity.mgmap.features.routing;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

public abstract class RoutingProfile {

    protected String id;

    public RoutingProfile(){
        id = constructId(this.getClass());
    }

    public static String constructId(Class<?> clazz){
        return "RP_"+clazz.getSimpleName();
    }

    public String getId(){
        return id;
    }

    public WayAttributs getWayAttributes(Way way){
        return new WayAttributs();
    }

    public void refreshWayAttributes(WayAttributs wayAttributs){}

    public double getCost(WayAttributs wayAttributs, GNode node1, GNode node2){
        if ((wayAttributs!=null) && (wayAttributs.routingProfileChanged)){
            refreshWayAttributes(wayAttributs);
            wayAttributs.routingProfileChanged = false;
        }
        double distance = PointModelUtil.distance(node1, node2);
        float verticalDistance = node2.getEleD() - node1.getEleD();
        return getCost(wayAttributs, distance, verticalDistance);
    }
    protected abstract double getCost(WayAttributs wayAttributs, double distance, float verticalDistance);

    public double heuristic(GNode node, GNode target){
        double distance = PointModelUtil.distance(node, target);
        float verticalDistance = target.getEleD() - node.getEleD();
        return heuristic(distance, verticalDistance);
    }
    protected double heuristic(double distance, float verticalDistance){
        return distance*0.999;
    }

    protected double acceptedRouteDistance(RoutingEngine routingEngine, PointModel pmStart, PointModel pmEnd){
        double distance = PointModelUtil.distance(pmStart, pmEnd);
        float verticalDistance = pmEnd.getEleD() - pmStart.getEleD();
        return acceptedRouteDistance(routingEngine, distance, verticalDistance);
    }
    protected double acceptedRouteDistance(RoutingEngine routingEngine, double distance, float verticalDistance){
        double res = 0;
        if (distance < routingEngine.getRoutingContext().maxBeelineDistance){ // otherwise it will take too long
            res = routingEngine.getRoutingContext().maxRouteLengthFactor * heuristic(distance,verticalDistance) + 2 * PointModelUtil.getCloseThreshold();
        }
        return res;
    }


    abstract protected int getIconIdActive();
    abstract protected int getIconIdInactive();


    void initETV(ExtendedTextView etv, Pref<String> prefCurrentRoutingProfileId){
        Pref<Boolean> rpState = new Pref<>(id.equals(prefCurrentRoutingProfileId.getValue()));
        prefCurrentRoutingProfileId.addObserver(evt -> rpState.setValue( id.equals(prefCurrentRoutingProfileId.getValue()) ));
        etv.setData(rpState, getIconIdInactive(), getIconIdActive());
        etv.setOnClickListener(v -> prefCurrentRoutingProfileId.setValue(id));
    }
}
