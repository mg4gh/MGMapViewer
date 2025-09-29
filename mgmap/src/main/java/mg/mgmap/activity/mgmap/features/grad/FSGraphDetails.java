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

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.impl2.ApproachModelImpl;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
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

        ApproachModel am = getActivity().getGraphFactory().calcApproach(pmTap, PointModelUtil.getCloseThreshold());
        ArrayList<? extends Graph> graphs =  getActivity().getGraphFactory().getGraphList(bBoxTap);

        Graph aGraph = graphs.get(0);
        if ((aGraph != null) && (am instanceof ApproachModelImpl ami)){
            for (PointModel pm : aGraph.segmentNodes(ami.getNode1(), ami.getNode2())){
                multiPointModel.addPoint(pm);
                mgLog.d("Point "+ pm + aGraph.getRefDetails(pm));
            }
        }
        aGraph = null;
        for (Graph graph : graphs) {
            if (graph.getBBox().contains(pmTap)) {
                MultiMultiPointView mmpv = new MultiMultiPointView(graph.getRawWays(), PAINT_GRAD_ALL_STROKE);
                mmpv.setShowIntermediates(true);
                register(mmpv); // show also the complete tile graph
                BoxView boxView = new BoxView(graph.getBBox(), PAINT_GRAD_ALL_STROKE);
                register(boxView);
            }
            if (graph.getBBox().contains(multiPointModel.getBBox())){
                aGraph = graph;
            }
        }
        return aGraph==null?null:aGraph.getBBox();
    }

}
