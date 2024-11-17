/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.routing;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Locale;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;

public class RouteOptimizer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final GGraphTileFactory gFactory;
    final RoutingEngine routingEngine;

    public RouteOptimizer(GGraphTileFactory gFactory, RoutingEngine routingEngine){
        this.gFactory = gFactory;
        this.routingEngine = routingEngine;
    }

    private RoutePointModel getRoutePointModel(PointModel pm){
        return routingEngine.getVerifyRoutePointModel( pm );
    }

    private void replaceNode(MultiPointModelImpl mpmi, int idx, PointModel pm){
        mpmi.removePoint(idx);
        mpmi.addPoint(idx,pm);
    }

    public boolean checkRoute(TrackLogSegment segment, int startIdx, int endIdx,
                              HashMap<PointModel, ApproachModel> matchingApproachMap){

        matchingApproachMap.clear();

        RoutePointModel rpmSource =  getRoutePointModel(segment.get(startIdx) );
        RoutePointModel rpmTarget =  getRoutePointModel(segment.get(endIdx) );

        MultiPointModelImpl route = routingEngine.calcRouting(rpmSource, rpmTarget);
        if (!route.isRoute()) return false;

        assert (rpmSource.getApproachNode() == route.get(0));
        assert (rpmTarget.getApproachNode() == route.get(route.size()-1));

        mgLog.i("startIdx="+startIdx+" endIdx="+endIdx);

        if (rpmSource.getApproach().getNode1() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode2());
        if (rpmSource.getApproach().getNode2() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode1());
        if (rpmTarget.getApproach().getNode1() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode2());
        if (rpmTarget.getApproach().getNode2() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode1());

        double[] rdist = new double[route.size()];
        rdist[0] = 0;
        for (int rIdx = 1; rIdx<route.size(); rIdx++) {
            rdist[rIdx] = rdist[rIdx-1] + PointModelUtil.distance(route.get(rIdx-1),route.get(rIdx));
        }

        BBox rbBox = new BBox();
        RoutePointModel lastRPM = routingEngine.getVerifyRoutePointModel( segment.get(startIdx) );
        double lastDist = PointModelUtil.distance(route.get(0), lastRPM.mtlp);
        for (int idx=startIdx+1; idx < endIdx; idx++){
            RoutePointModel rpm = routingEngine.getVerifyRoutePointModel( segment.get(idx) );
            for (ApproachModel approachModel : rpm.getApproaches()) {
                routingEngine.validateApproachModel(approachModel); // renew node1 and node2, if GGraphTile was newly setup due to cache effect
            }

            double optimalDist = lastDist + PointModelUtil.distance(lastRPM.mtlp, rpm.mtlp);
            ApproachModel match = null;
            double matchDist = Double.MAX_VALUE;

            for (int rIdx = 1; rIdx<route.size(); rIdx++){ //iterate over route parts - rIdx determines endpoint of part
                rbBox.clear();
                if (rpm.approachBBox.intersects( rbBox.extend(route.get(rIdx)).extend(route.get(rIdx-1))  )){ // ok, it might give a route match
                    for (ApproachModel approachModel : rpm.getApproaches()){
                        routingEngine.validateApproachModel(approachModel); // renew node1 and node2, if GGraphTile was newly setup due to cache effect
                        if (((approachModel.getNode1() == route.get(rIdx)) && (approachModel.getNode2() == route.get(rIdx-1))) ||
                            ((approachModel.getNode2() == route.get(rIdx)) && (approachModel.getNode1() == route.get(rIdx-1))))   {
                            double currentDist = rdist[rIdx-1]+PointModelUtil.distance(route.get(rIdx-1),approachModel.getApproachNode());
                            if (currentDist > lastDist){
                                if (Math.abs(currentDist-optimalDist) < Math.abs(matchDist-optimalDist)){
                                    mgLog.i("idx="+idx+" lastDist="+lastDist+" optimalDist="+optimalDist+" matchDist="+matchDist+" currentDist="+currentDist+ " cost="+approachModel.getApproachNode().getNeighbour().getCost());
                                    matchDist = currentDist;
                                    match = approachModel;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (match == null) return false;
            lastDist = matchDist;
            lastRPM = rpm;
            matchingApproachMap.put(rpm.mtlp, match);
        }
        return true;
    }



    public void optimize(TrackLog trackLog){
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            optimize(segment);
        }
    }

    private class Scorer{
        private final int hops;
        private final int jump;
        private final double max;
        private final HashMap<PointModel, ApproachModel> checkResults = new HashMap<>();

        int next=0;
        private Scorer(int hops, int jump, double max) {
            this.hops = hops;
            this.jump = jump;
            this.max = max;
        }

        private void score(TrackLogSegment segment, int idx){
            if (idx == next){ // do scoring
                mgLog.i(hops+" "+idx);
                int end = idx;
                double dist = 0;
                for (int cnt=1; (cnt<=hops) && (idx+cnt<segment.size()); cnt++){
                    double d = getRoutePointModel(segment.get(idx+cnt)).currentDistance;
                    if (dist + d < max) {
                        dist += d;
                        end = idx+cnt;
                    }
                }
                if (end > idx){
                    checkResults.clear();
                    double avgApproachDistance = 0;
                    if (checkRoute(segment, idx, end, checkResults)){
                        for (PointModel pm : checkResults.keySet()){
                            ApproachModel am = checkResults.get(pm);
                            if (am != null) avgApproachDistance += am.getApproachNode().getNeighbour().getCost();
                        }
                        avgApproachDistance /= checkResults.keySet().size();

                        double score = 1 - (0.5 * avgApproachDistance / routingEngine.getRoutingContext().approachLimit); // 0.5 .. 1 score point depending an avgApproachDistance

                        for (int ii=idx+1; ii<end; ii++ ){
                            int f = Math.min(ii-idx, end-ii);
                            PointModel pm = segment.get(ii);
                            ApproachModel am = checkResults.get(pm);
                            if (am != null){
                                double amScore  = getScore(am);
                                scoreMap.put(am, amScore + score*f);
                                RoutePointModel rpm = routingEngine.getRoutePointMap().get(pm);
                                if (rpm != null){
                                    mgLog.d("ii="+ii+" dist="+am.getApproachNode().getNeighbour().getCost()+" aIdx="+rpm.getApproaches().indexOf(am)+" +"+(score*f));
                                }
                            }
                        }
                    }
                }
                next += jump;
            }

        }
    }


    private final HashMap<ApproachModel, Double> scoreMap = new HashMap<>();
    private double getScore(ApproachModel am){
        Double dScore = scoreMap.get(am);
        return (dScore != null)? dScore : 0;
    }

    public void optimize(TrackLogSegment segment){
        Scorer s1 = new Scorer(7,4,1000);
        Scorer s2 = new Scorer(17,7,2000);
        Scorer s3 = new Scorer(37,13,2000);

        for (int idx=0; idx<segment.size(); idx++){
            s1.score(segment, idx);
            s2.score(segment, idx);
            s3.score(segment, idx);
            PointModel pm = segment.get(idx);
            if (pm instanceof WriteablePointModel mtlp) {
                RoutePointModel rpm = getRoutePointModel(pm);
                double highScore = 0;
                StringBuilder log = new StringBuilder();
                for (ApproachModel am : rpm.getApproaches()){
                    double amScore  = getScore(am);
                    log.append(String.format(Locale.ENGLISH, " %.2f", amScore));
                    if (amScore > highScore){
                        highScore = amScore;
                        rpm.selectedApproach = am;
                        log.append("+");
                    }
                }
                mgLog.i("idx="+idx+" "+segment.get(idx)+ log);
                PointModel optimizedPM = rpm.getApproachNode();
                if (optimizedPM != null){
                    mtlp.setLat(optimizedPM.getLat());
                    mtlp.setLon(optimizedPM.getLon());
                    routingEngine.routePointMap.remove(mtlp);
                }
            }
        }
    }

}
