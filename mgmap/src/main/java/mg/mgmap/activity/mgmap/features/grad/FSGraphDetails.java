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
import java.util.Locale;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
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
        BBox bBox = null;

        ApproachModel am = getActivity().getGraphFactory().calcApproach(pmTap, (int)closeThreshold);
        Graph graph = getActivity().getGraphFactory().getGraphTile(am);

        PointModel lastPm = null;
        if (graph != null){
            bBox = graph.getBBox();
            for (PointModel pm : graph.segmentNodes(am)){
                multiPointModel.addPoint(pm);
                if (lastPm != null){
                    double dist = PointModelUtil.distance(lastPm,pm);
                    double vertDist = PointModelUtil.verticalDistance(lastPm,pm);
                    double rise = 100 * vertDist/dist;
                    mgLog.d(String.format(Locale.ENGLISH, "    dist=%.2fm vertDist=%.1fm rise=%.1f",dist,vertDist,rise));
                }
                mgLog.d("Point "+ pm + graph.getRefDetails(pm));
                lastPm = pm;
            }
            MultiMultiPointView mmpv = new MultiMultiPointView(graph.getRawWays(), PAINT_GRAD_ALL_STROKE);
            mmpv.setShowIntermediates(true);
            register(mmpv); // show also the complete tile graph
            BoxView boxView = new BoxView(graph.getBBox(), PAINT_GRAD_ALL_STROKE);
            register(boxView);
        }
        return bBox;
    }

}
