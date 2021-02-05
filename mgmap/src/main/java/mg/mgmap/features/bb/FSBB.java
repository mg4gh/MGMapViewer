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
package mg.mgmap.features.bb;

import android.util.DisplayMetrics;
import android.util.Log;

import org.mapsforge.map.layer.Layer;

import java.util.ArrayList;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.MGMapLayerFactory;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.features.tilestore.MGTileStore;
import mg.mgmap.features.tilestore.MGTileStoreLayer;
import mg.mgmap.features.tilestore.TileStoreLoader;
import mg.mgmap.model.BBox;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.model.WriteablePointModelImpl;
import mg.mgmap.util.NameUtil;
import mg.mgmap.model.PointModelUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.MVLayer;

public class FSBB extends FeatureService {

    private final Pref<Boolean> triggerBboxOn = new Pref<>(false);
    private final Pref<Boolean> prefBboxOn = getPref(R.string.FSBB_qc_bboxOn, false);

    private final Pref<Boolean> triggerLoadFromBB = new Pref<>(false);
    private final Pref<Boolean> prefLoadFromBBEnabled = new Pref<>(false);
    private final Pref<Boolean> triggerTSLoadRemain = new Pref<>(false);
    private final Pref<Boolean> prefTSActionsEnabled = new Pref<>(false);
    private final Pref<Boolean> triggerTSLoadAll = new Pref<>(false);
    private final Pref<Boolean> triggerTSDeleteAll = new Pref<>(false);

    private final ArrayList<MGTileStore> tss = identifyTS();
    private boolean initSquare = false;

    public FSBB(MGMapActivity mmActivity) {
        super(mmActivity);

        triggerBboxOn.addObserver( (o, args) -> prefBboxOn.toggle());
        triggerLoadFromBB.addObserver( (o, args) -> loadFromBB());
        triggerTSLoadRemain.addObserver( (o, args) -> tsAction(false, false));
        triggerTSLoadAll.addObserver( (o, args) -> tsAction(false, true));
        triggerTSDeleteAll.addObserver( (o, args) -> tsAction(true, true));
        prefBboxOn.addObserver(refreshObserver);
    }

    private WriteablePointModel p1 = null;
    private WriteablePointModel p2 = null;
    private final Runnable ttHide = () -> prefBboxOn.setValue(false);
    long ttHideTime = 30000;
    private void refreshTTHide(){
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide,ttHideTime);
    }
    public void cancelTTHide(){
        getTimer().removeCallbacks(ttHide);
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("group_bbox".equals(info)){
            etv.setPrAction(new Pref<>(false));
            etv.setData(prefBboxOn,R.drawable.group_bbox1,R.drawable.group_bbox2);
        } else if ("loadFromBB".equals(info)){
            etv.setPrAction(triggerLoadFromBB);
            etv.setData(R.drawable.load_from_bb);
            etv.setDisabledData(prefLoadFromBBEnabled, R.drawable.load_from_bb_dis);
            etv.setHelp(r(R.string.FSBB_qcLoadFromBB_Help));
        } else if ("bbox_on".equals(info)){
            etv.setPrAction(triggerBboxOn);
            etv.setData(prefBboxOn,R.drawable.bbox2,R.drawable.bbox);
            etv.setHelp(r(R.string.FSBB_qcBBox_Help)).setHelp(r(R.string.FSBB_qcBBox_Help1),r(R.string.FSBB_qcBBox_Help2));
        } else if ("TSLoadRemain".equals(info)){
            etv.setPrAction(triggerTSLoadRemain);
            etv.setData(R.drawable.bb_ts_load_remain);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_load_remain_dis);
            etv.setHelp(r(R.string.FSBB_qcTSLoadRemainFromBB_Help));
        }else if ("TSLoadAll".equals(info)){
            etv.setPrAction(triggerTSLoadAll);
            etv.setData(R.drawable.bb_ts_load_all);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_load_all_dis);
            etv.setHelp(r(R.string.FSBB_qcTSLoadAllFromBB_Help));
        }else if ("TSDeleteAll".equals(info)){
            etv.setPrAction(triggerTSDeleteAll);
            etv.setData(R.drawable.bb_ts_delete_all);
            etv.setDisabledData(prefTSActionsEnabled, R.drawable.bb_ts_delete_all_dis);
            etv.setHelp(r(R.string.FSBB_qcTSDeleteAllFromBB_Help));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefBboxOn.setValue(false);
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        if (prefBboxOn.getValue()){
            if (bbcl == null){
                bbcl = new BBControlLayer();
                register(bbcl, false);
                initSquare = true;
                refreshObserver.onChange();
            } else if (initSquare){
                if (bbcl.initFromScreen()){
                    initSquare = false;
                } else {
                    refreshObserver.onChange();
                }
            }
        } else {
            hideBB();
        }
        prefLoadFromBBEnabled.setValue(isLoadAllowed());
        boolean tsOpsAllowed = isLoadAllowed() && (tss.size() > 0);
        prefTSActionsEnabled.setValue(tsOpsAllowed);
    }

    public class BBControlLayer extends MVLayer {

        private BBControlLayer(){
            setDragging();
            changed();
        }

        @Override
        protected boolean onTap(WriteablePointModel point) {
            if (p2 == null){
                p2 = point;
                changed();
                return true;
            } else if (p1 == null){
                p1 = point;
                changed();
                return true;
            }
            return false;
        }

        @Override
        protected boolean checkDrag(PointModel pmStart, DragData dragData) {
            double dp1 = Double.MAX_VALUE;
            double dp2 = Double.MAX_VALUE;
            // 3 Fälle:
            //   1.) p1 wird verschoben
            //   2.) p2 wird verschoben
            //   3.) noch keine Punkte gesetzt und es wird ein Fenster aufgezogen
            if (p1 != null){
                dp1 = PointModelUtil.distance(p1, pmStart);
            }
            if (p2 != null){
                dp2 = PointModelUtil.distance(p2, pmStart);
            }

            double close = getMapViewUtility().getCloseThreshouldForZoomLevel();
            if ((dp1 < close) && (dp2 > close)){
                dragData.setDragObject(p1);
            }
            if ((dp2 < close) && (dp1 > close)){
                dragData.setDragObject(p2);
            }

            if ((p1 == null) && (p2 == null)){
                p1 = new WriteablePointModelImpl(pmStart);
                p2 = new WriteablePointModelImpl(pmStart);
                dragData.setDragObject(p1);
            }
            return (dragData.getDragObject() != null);
        }

        @Override
        protected void handleDrag(PointModel pmCurrent, DragData dragData) {
            WriteablePointModel pmDrag = dragData.getDragObject(WriteablePointModel.class);
            pmDrag.setLat(pmCurrent.getLat());
            pmDrag.setLon(pmCurrent.getLon());
            changed();
        }


        public boolean initFromScreen(){
            if (topLeftPoint == null) return false;
            DisplayMetrics dm = getApplication().getApplicationContext().getResources().getDisplayMetrics();
            double x1 = dm.widthPixels / 3.0;
            double x2 = x1 * 2;
            double y1 = (dm.heightPixels / 2.0) - (x1 / 2);
            double y2 = (dm.heightPixels / 2.0) + (x1 / 2);
            p1 = new WriteablePointModelImpl(y2lat(y1),x2lon(x1));
            p2 = new WriteablePointModelImpl(y2lat(y2),x2lon(x2));
            changed();
            return true;
        }

        @Override
        protected boolean onLongPress(PointModel pm) {
            if ((p1!= null) && (p2!= null)){
                BBox bBox = new BBox().extend(p1).extend(p2);
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " bBox="+bBox);
                if (bBox.contains(pm)){
                    if (loadFromBB(bBox)){
                        prefBboxOn.setValue(false);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void changed(){
        unregisterAll();
        if (p1 != null){
            register(new PointViewBB(p1));
        }
        if (p2 != null){
            register(new PointViewBB(p2));
        }
        if ((p1 != null) && (p2 != null)){
            register(new BoxViewBB(new BBox().extend(p1).extend(p2)));
        }
        refreshTTHide();
    }


    private BBControlLayer bbcl = null;

    public BBox getBBox(){
        return new BBox().extend(p1).extend(p2);
    }

    public void loadFromBB(){
        if ((p1!= null) && (p2!= null)){
            BBox bBox = new BBox().extend(p1).extend(p2);
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " bBox="+bBox);
            loadFromBB(bBox);
        }
    }

     void hideBB(){
        p1 = null;
        p2 = null;
        if (bbcl != null){
            unregister(bbcl, false);
        }
        bbcl = null;
        unregisterAll();
        cancelTTHide();
    }

    boolean isLoadAllowed(){
        return ((p1 != null) && (p2 != null) && (bbcl!= null));
    }

    private ArrayList<MGTileStore> identifyTS(){
        ArrayList<MGTileStore> tss = new ArrayList<>();
        for (Layer layer : MGMapLayerFactory.mapLayers.values()){
            if (layer instanceof MGTileStoreLayer) {
                MGTileStoreLayer mgTileStoreLayer = (MGTileStoreLayer) layer;
                MGTileStore mgTileStore = mgTileStoreLayer.getMGTileStore();
                if (mgTileStore.hasConfig()){
                    tss.add(mgTileStore);
                }
            }
        }
        return tss;
    }

    private void tsAction(boolean bDrop, boolean bAll){
        for (MGTileStore ts : tss){
            try {
                TileStoreLoader tileStoreLoader = new TileStoreLoader(getActivity(), getApplication(), ts);
                if (bDrop){
                    tileStoreLoader.dropFromBB(getBBox());
                } else {
                    tileStoreLoader.loadFromBB(getBBox(), bAll);
                }

            } catch (Exception e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            }
        }
    }

    public boolean loadFromBB(BBox bBox2Load){
        boolean changed = false;
        BBox bBox2show = new BBox();
        if (bBox2Load != null){
            for (TrackLog aTrackLog : getApplication().metaTrackLogs.values()){
                if (getApplication().getMetaDataUtil().checkLaLoRecords(aTrackLog, bBox2Load)){
                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(aTrackLog);
                    bBox2show.extend(aTrackLog.getBBox());
                    changed = true;
                }
            }
            if (changed){
                getApplication().availableTrackLogsObservable.changed();
//                getMapViewUtility().zoomForBoundingBox(bBox2show);
            }
        }
        return changed;
    }

}
