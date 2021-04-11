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

import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.basic.Assert;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.PointModelUtil;

public class RouteOptimizer {

    GGraphTileFactory gFactory;
    RoutingEngine routingEngine;

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

        Assert.check(rpmSource.getApproachNode() == route.get(0));
        Assert.check(rpmTarget.getApproachNode() == route.get(route.size()-1));

        Log.i(MGMapApplication.LABEL, NameUtil.context());

        if (rpmSource.getApproach().getNode1() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode2());
        if (rpmSource.getApproach().getNode2() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode1());
        if (rpmTarget.getApproach().getNode1() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode2());
        if (rpmTarget.getApproach().getNode2() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode1());





        for (int idx=startIdx+1; idx < endIdx; idx++){
            RoutePointModel rpm = routingEngine.getVerifyRoutePointModel( segment.get(idx) );

            ApproachModel match = null;

            for (int rIdx = 1; rIdx<route.size(); rIdx++){ //iterate over route parts - rIdx determines endpoint of part
                if (rpm.approachBBox.contains(route.get(rIdx))){ // ok, it might give a route match
                    for (ApproachModel approachModel : rpm.getApproaches()){
                        if (approachModel == match) break; // can't improve
                        gFactory.validateApproachModel(approachModel); // renew node1 and node2, if GGraphTile was newly setup due to cache effect
                        if (((approachModel.getNode1() == route.get(rIdx)) && (approachModel.getNode2() == route.get(rIdx-1))) ||
                            ((approachModel.getNode2() == route.get(rIdx)) && (approachModel.getNode1() == route.get(rIdx-1))))   {
                            match = approachModel;
                            break;
                        }
                    }
                }
            }
            if (match == null) return false;
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
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+hops+" "+idx);
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

                        double score = 1 - (0.5 * avgApproachDistance / PointModelUtil.getCloseThreshold()); // 0.5 .. 1 score point depending an avgApproachDistance

//TODO: Understand this old code ... (... unfortunately produces a null pointer exception)
//                        for (int i=idx+1; i<=end; i++){
//                            if (!getRoutePointModel(segment.get(i)).currentMPM.isRoute()){
//                                score *= 10; // solution for a jumpLine? => set a high score
//                            }
//                        }

                        for (PointModel pm : checkResults.keySet()){
                            ApproachModel am = checkResults.get(pm);
                            double amScore  = getScore(am);
                            scoreMap.put(am, amScore+score);
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
        Scorer s1 = new Scorer(3,2,500);
        Scorer s2 = new Scorer(7,4,1000);
        Scorer s3 = new Scorer(17,7,2000);

        for (int idx=0; idx<segment.size(); idx++){
            s1.score(segment, idx);
            s2.score(segment, idx);
            s3.score(segment, idx);
            RoutePointModel rpm = getRoutePointModel(segment.get(idx));
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
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"idx="+idx+" "+segment.get(idx)+ log);
        }
    }

}
