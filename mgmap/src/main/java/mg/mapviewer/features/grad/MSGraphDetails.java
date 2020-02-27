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
package mg.mapviewer.features.grad;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;

import java.util.ArrayList;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.graph.GGraphTile;
import mg.mapviewer.graph.GNeighbour;
import mg.mapviewer.graph.GNode;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.MultiPointModelImpl;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.view.BoxView;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.view.MultiMultiPointView;
import mg.mapviewer.view.MultiPointView;

public class MSGraphDetails extends MGMicroService {

    private static final Paint PAINT_GRAD_STROKE =  CC.getStrokePaint(R.color.RED_A150, 3);
    public static final Paint PAINT_GRAD_ALL_STROKE = CC.getStrokePaint(R.color.GRAY100_A150, 3);

    public class GradControlLayer extends MVLayer{
        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            unregisterAll();
            return (showGraphDetails( pmTap ));
        }
    }

    private GradControlLayer controlLayer = null;

    public MSGraphDetails(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {
        getApplication().wayDetails.addObserver(refreshObserver);

        Log.i(MGMapApplication.LABEL, NameUtil.context()+" wayDetails=" + getApplication().wayDetails.getValue());
        if (getApplication().wayDetails.getValue()){
            controlLayer = new GradControlLayer();
            register(controlLayer, false);
        }

    }

    @Override
    protected void stop() {
        getApplication().wayDetails.deleteObserver(refreshObserver);

        unregisterClass(GradControlLayer.class);
        unregisterAll();
        controlLayer = null;
    }

    @Override
    protected void doRefresh() {
        if (getApplication().wayDetails.getValue()){    // should be active
            if (controlLayer == null){                  // but is not yet
                start();                                // therefore start it
            }
        } else {
            if (controlLayer != null){
                stop();
            }
        }
    }


    private boolean showGraphDetails(PointModel pmTap){
        if (getApplication().wayDetails.getValue()){
            MultiPointModelImpl multiPointModel = new MultiPointModelImpl();
            BBox bBox = getGraphDetails(pmTap, multiPointModel);
            if (bBox != null){
                BoxView boxView = new BoxView(bBox, PAINT_GRAD_STROKE);
                register(boxView);
                MultiPointView multiPointView = new MultiPointView(multiPointModel, PAINT_GRAD_STROKE);
                multiPointView.setShowIntermediates(true);
                register(multiPointView);
//                return true;
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
