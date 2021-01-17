/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mgmap.features.grad;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;

import java.util.ArrayList;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.graph.GGraphTile;
import mg.mgmap.graph.GNeighbour;
import mg.mgmap.graph.GNode;
import mg.mgmap.model.BBox;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.util.CC;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.view.BoxView;
import mg.mgmap.view.MVLayer;
import mg.mgmap.view.MultiMultiPointView;
import mg.mgmap.view.MultiPointView;

public class FSGraphDetails extends FeatureService {

    private static final Paint PAINT_GRAD_STROKE =  CC.getStrokePaint(R.color.RED_A150, 3);
    public static final Paint PAINT_GRAD_ALL_STROKE = CC.getStrokePaint(R.color.GRAY100_A150, 3);

    public class GradControlLayer extends MVLayer{
        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            unregisterAll();
            if (getMapView().getModel().mapViewPosition.getZoomLevel() <= 13) return false;
            return (showGraphDetails( pmTap ));
        }
    }

    private GradControlLayer controlLayer = null;

    private final Pref<Boolean> prefWayDetails = getPref(R.string.FSGrad_pref_WayDetails_key, false);

    public FSGraphDetails(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void onResume() {
        if (prefWayDetails.getValue()){
            controlLayer = new GradControlLayer();
            register(controlLayer, false);
        }
    }

    @Override
    protected void onPause() {
        unregisterClass(GradControlLayer.class);
        unregisterAll();
        controlLayer = null;
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

        double closeThreshold = getMapViewUtility().getCloseThreshouldForZoomLevel();
        BBox bBoxTap = new BBox()
                .extend(pmTap)
                .extend(closeThreshold);

        GNode bestNode = null;
        GNeighbour bestNeighbour = null;
        GGraphTile bestTile = null;
        WriteablePointModel pmApproach = new TrackLogPoint();
        double bestDistance = closeThreshold;
        ArrayList<GGraphTile> tiles = GGraphTile.getGGraphTileList(getActivity().getMapDataStore(bBoxTap), bBoxTap);

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
                    if (PointModelUtil.compareTo(node, neighbourNode) < 0){ // neighbour relations exist in both direction - here we can reduce to one
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



        if ((bestNode != null) && (bestNeighbour != null) && (bestTile != null)){
            ArrayList<GNode> nodes = bestTile.segmentNodes(bestNode,bestNeighbour.getNeighbourNode(),Integer.MAX_VALUE);

            for (GNode node : nodes){
                multiPointModel.addPoint(node);
                Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Point "+ node);
            }
        }
        return (bestTile==null)?null:bestTile.getTileBBox();
    }

}
