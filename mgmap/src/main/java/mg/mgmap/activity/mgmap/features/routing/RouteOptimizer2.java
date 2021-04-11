package mg.mgmap.activity.mgmap.features.routing;

import android.util.Log;

import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.basic.Assert;
import mg.mgmap.generic.util.basic.NameUtil;

public class RouteOptimizer2 {

    GGraphTileFactory gFactory;
    RoutingEngine routingEngine;

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
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" current="+current+" upto="+upto);
            for (int currentEnd = upto; currentEnd > current; currentEnd-=3){
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" current="+current+" currentEnd="+currentEnd);

                RoutePointModel rpmSource =  getRoutePointModel(segment.get(current) );
                RoutePointModel rpmTarget =  getRoutePointModel(segment.get(currentEnd) );

                MultiPointModelImpl route = routingEngine.calcRouting(rpmSource, rpmTarget);
                if (!route.isRoute()) continue;

                Assert.check(rpmSource.getApproachNode() == route.get(0));
                Assert.check(rpmTarget.getApproachNode() == route.get(route.size()-1));

                boolean fits = true;
                for (int idx=current+1; idx<currentEnd; idx++){
                    double maxDistance = PointModelUtil.getCloseThreshold()*1.2;
                    TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1, maxDistance );
                    PointModelUtil.getBestDistance(route,segment.get(idx),bestMatch);
                    if (bestMatch.getDistance() == maxDistance) {
                        fits = false;
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" fits failed idx="+idx);
                        break;
                    }
                }
                if (fits){
                    idx2keep.add(currentEnd);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" idx2keep add idx="+currentEnd);
                    break;
                }
            }
            current = Math.max(current+1, idx2keep.get(idx2keep.size()-1));
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
