package mg.mgmap.activity.mgmap.features.routing;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.basic.MGLog;

public class RouteOptimizer2 {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final GGraphTileFactory gFactory;
    final RoutingEngine routingEngine;

    public RouteOptimizer2(GGraphTileFactory gFactory, RoutingEngine routingEngine){
        this.gFactory = gFactory;
        this.routingEngine = routingEngine;
    }

    private RoutePointModel getRoutePointModel(PointModel pm){
        return routingEngine.getVerifyRoutePointModel( pm );
    }

    public void optimize(TrackLog trackLog){
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            optimize(segment);
        }
    }

    public void optimize(TrackLogSegment segment){
        int current = 0;
        ArrayList<Integer> idx2keep = new ArrayList<>();
        idx2keep.add(current);
        while (current < segment.size()){

            int upto = current;
            double dist = 0;
            for (int cnt=1; ((current+cnt)<segment.size() && (cnt <= 50)); cnt++){
                double d = PointModelUtil.distance(segment.get(cnt-1),segment.get(cnt));
                if (dist + d < 1000.0) {
                    dist += d;
                    upto = current+cnt;
                } else {
                    break;
                }
            }
            mgLog.i("current="+current+" upto="+upto);
            int minStep = 3;
            for (int currentEnd = upto; currentEnd > current+minStep-1; currentEnd-=minStep){
                mgLog.i("current="+current+" currentEnd="+currentEnd);

                RoutePointModel rpmSource =  getRoutePointModel(segment.get(current) );
                RoutePointModel rpmTarget =  getRoutePointModel(segment.get(currentEnd) );

                MultiPointModelImpl route = routingEngine.calcRouting(rpmSource, rpmTarget);
                if (!route.isRoute()) continue;

                assert (rpmSource.getApproachNode().getLaLo() == route.get(0).getLaLo());
                assert (rpmTarget.getApproachNode().getLaLo() == route.get(route.size()-1).getLaLo());

                boolean fits = true;
                for (int idx=current+1; idx<currentEnd; idx++){
                    double maxDistance = routingEngine.getRoutingContext().approachLimit*1.2;
                    TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1, maxDistance );
                    PointModelUtil.getBestDistance(route,segment.get(idx),bestMatch);
                    if (bestMatch.getDistance() == maxDistance) {
                        fits = false;
                        mgLog.i("fits failed idx="+idx);
                        break;
                    }
                }
                if (fits){
                    idx2keep.add(currentEnd);
                    mgLog.i("idx2keep add idx="+currentEnd);
                    break;
                }
            }
            int lastIdx2keep = idx2keep.get(idx2keep.size()-1);
            if (current == lastIdx2keep){
                current = current+minStep;
                idx2keep.add(current);
            } else {
                current = lastIdx2keep;
            }
//            current = Math.min(Math.max(current+5, lastIdx2keep) , segment.size()-1);
        }
        if (idx2keep.get(idx2keep.size()-1) != (segment.size()-1)){
            idx2keep.add( segment.size()-1 );
        }
        for (int idx=(segment.size()-1); idx >=0; idx--){
            if (!idx2keep.contains(idx)){
                segment.removePoint(idx);
            }
        }

    }

}
