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
package mg.mgmap.activity.mgmap.features.grad;

import org.mapsforge.core.graphics.Paint;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.routing.FSRouting;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.graph.GGraphTile;
import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.GNodeRef;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.activity.mgmap.view.BoxView;
import mg.mgmap.activity.mgmap.view.MultiMultiPointView;
import mg.mgmap.activity.mgmap.view.MultiPointView;

public class FSGraphDetails extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final Paint PAINT_GRAD_STROKE =  CC.getStrokePaint(R.color.CC_RED_A150, 3);
    public static final Paint PAINT_GRAD_ALL_STROKE = CC.getStrokePaint(R.color.CC_GRAY100_A150, 3);

    public class GradControlLayer extends ControlMVLayer<Object> {
        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            unregisterAll();
            final boolean[] res = {false};
            if (verifyGraphDetailsVisibility()) {
                new Thread(() -> res[0] = showGraphDetails( pmTap )).start();
            }
            return res[0];
        }
    }

    private GradControlLayer controlLayer = null;

    private final Pref<Boolean> prefWayDetails = getPref(R.string.FSGrad_pref_WayDetails_key, false);
    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);

    public FSGraphDetails(MGMapActivity mmActivity) {
        super(mmActivity);
        prefZoomLevel.addObserver(refreshObserver);
    }

    @Override
    protected void onResume() {
        if (prefWayDetails.getValue()){
            controlLayer = new GradControlLayer();
            register(controlLayer);
        }
    }

    @Override
    protected void onPause() {
        unregisterAllControl();
        unregisterAll();
        controlLayer = null;
    }

    @Override
    protected void doRefreshResumedUI() {
        if (!verifyGraphDetailsVisibility()){
            unregisterAll();
        }
    }

    // GraphDetails shell be visible as long as zoom level is more than 13
    private boolean verifyGraphDetailsVisibility(){
        return prefZoomLevel.getValue() > 13;
    }

    private boolean showGraphDetails(PointModel pmTap){
        if (prefWayDetails.getValue()){
            MultiPointModelImpl multiPointModel = new MultiPointModelImpl();
            BBox bBox = getGraphDetails(pmTap, multiPointModel);
            if (bBox != null){
                BoxView boxView = new BoxView(bBox, PAINT_GRAD_STROKE);
                register(boxView);
                MultiPointView multiPointView = new MultiPointView(multiPointModel, PAINT_GRAD_STROKE);
                multiPointView.setShowIntermediates(true);
                register(multiPointView);
            }
        }
        return false;
    }

    private BBox getGraphDetails(PointModel pmTap, MultiPointModelImpl multiPointModel){

        double closeThreshold = getMapViewUtility().getCloseThresholdForZoomLevel(500);
        BBox bBoxTap = new BBox()
                .extend(pmTap)
                .extend(closeThreshold);

        GNode bestNode = null;
        GNeighbour bestNeighbour = null;
        GGraphTile bestTile = null;
        WriteablePointModel pmApproach = new TrackLogPoint();
        double bestDistance = closeThreshold;
        ArrayList<GGraphTile> tiles = getActivity().getFS(FSRouting.class).getGGraphTileList(bBoxTap);

        for (GGraphTile gGraphTile : tiles){
            if (gGraphTile.getTileBBox().contains(pmTap)){
                MultiMultiPointView mmpv = new MultiMultiPointView(gGraphTile.getRawWays(), PAINT_GRAD_ALL_STROKE);
                mmpv.setShowIntermediates(true);
                register( mmpv ); // show also the complete tile graph
                BoxView boxView = new BoxView(gGraphTile.getTileBBox(), PAINT_GRAD_ALL_STROKE);
                register(boxView);
            }
            for (GNode node : gGraphTile.getNodes()) {

                GNeighbour neighbour = node.getNeighbour();
                while ((neighbour = gGraphTile.getNextNeighbour(node, neighbour)) != null) {
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (GNode.sameTile(node,neighbourNode) && (PointModelUtil.compareTo(node, neighbourNode) < 0)){ // neighbour relations exist in both direction - here we can reduce to one
                        BBox bBoxPart = new BBox().extend(node).extend(neighbourNode);

                        boolean bIntersects = bBoxTap.intersects(bBoxPart);
                        if (bIntersects){ // ok, is candidate for close
                            if (PointModelUtil.findApproach(pmTap, node, neighbourNode, pmApproach)){
                                double distance = PointModelUtil.distance(pmTap, pmApproach);

                                if (distance < bestDistance){ // ok, new best found
                                    bestNode = node;
                                    bestNeighbour = neighbour;
                                    bestDistance = distance;
                                    bestTile = gGraphTile;
                                }
                            }
                        }
                    }
                }
            }
        }

        //noinspection ConstantConditions
        if ((bestNode != null) && (bestNeighbour != null) && (bestTile != null)){ // second and third condition are automatically true
            ArrayList<GNode> nodes = bestTile.segmentNodes(bestNode,bestNeighbour.getNeighbourNode(),Integer.MAX_VALUE, true);

            GNode lastNode = null;
            for (GNode node : nodes){
                multiPointModel.addPoint(node);
                if (lastNode != null){
                    final GNode last = lastNode;
                    double distance = PointModelUtil.distance(last,node);
                    double verticalDistance = PointModelUtil.verticalDistance(last, node);
                    mgLog.d(()-> String.format(Locale.ENGLISH, "   segment dist=%.2f vertDist=%.2f ascend=%.1f cost=%.2f revCost=%.2f wa=%s",distance,verticalDistance,verticalDistance*100/distance,last.getNeighbour(node).getCost(),node.getNeighbour(last).getCost(),last.getNeighbour(node).getWayAttributs().toDetailedString()));
                }
                mgLog.d(()-> "Point "+ node + getRefDetails(node.getNodeRef()) + getRefDetails(node.getNodeRef(true)));
                lastNode = node;
            }
        }
        return (bestTile==null)?null:bestTile.getTileBBox();
    }

    private String getRefDetails(GNodeRef ref){
        if (ref == null) return "";
        return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",ref.isReverse()?"rv":"fw",ref.isSetteled(),ref.getCost(),ref.getHeuristic(),ref.getHeuristicCost());
    }
}
