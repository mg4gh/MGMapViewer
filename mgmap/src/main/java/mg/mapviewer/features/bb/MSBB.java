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
package mg.mapviewer.features.bb;

import android.util.DisplayMetrics;
import android.util.Log;

import java.util.TimerTask;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.atl.MSAvailableTrackLogs;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteablePointModelImpl;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.view.PrefTextView;

public class MSBB extends MGMicroService {

    private final MGPref<Boolean> prefBboxOn = MGPref.get(R.string.MSBB_qc_bboxOn, false);
    private final MGPref<Boolean> prefBboxCenter = MGPref.get(R.string.MSBB_qc_bboxCenter, true);

    private MSAvailableTrackLogs msAvailableTrackLogs;
    public MSBB(MGMapActivity mmActivity, MSAvailableTrackLogs msAvailableTrackLogs) {
        super(mmActivity);
        this.msAvailableTrackLogs = msAvailableTrackLogs;
    }

    private WriteablePointModel p1 = null;
    private WriteablePointModel p2 = null;
    private TimerTask ttHide = new TimerTask() {
        @Override
        public void run() {
            prefBboxOn.setValue(false);
        }
    };
    long ttHideTime = 15000;
    private void refreshTTHide(){
        getTimer().removeCallbacks(ttHide);
        getTimer().postDelayed(ttHide,ttHideTime);
    }

    @Override
    public void initQuickControl(PrefTextView ptv, String info){
        ptv.setPrefData(new MGPref[]{prefBboxOn,prefBboxCenter},
                new int[]{},
                new int[]{R.drawable.bbox,R.drawable.bbox2,R.drawable.bbox,R.drawable.bbox2});
    }

    @Override
    protected void start() {
        super.start();
        prefBboxOn.addObserver(refreshObserver);
        prefBboxCenter.addObserver(refreshObserver);
        prefBboxOn.setValue(false);
    }

    @Override
    protected void stop() {
        super.stop();
        prefBboxOn.deleteObserver(refreshObserver);
        prefBboxCenter.deleteObserver(refreshObserver);
    }

    public void triggerRefresh(){
        refreshObserver.onChange();
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (prefBboxCenter.getValue()){
                    prefBboxOn.setValue(true);
                }
                if (prefBboxOn.getValue()){
                    if (bbcl == null){
                        bbcl = new BBControlLayer();
                        register(bbcl, false);
                        triggerRefresh();
                    } else if (prefBboxCenter.getValue()){
                        if (bbcl.initFromScreen()){
                            prefBboxCenter.setValue(false);
                        } else {
                            triggerRefresh();
                        }
                    }
                } else {
                    hideBB();
                }
                getControlView().updateTvZoom(getMapView().getModel().mapViewPosition.getZoomLevel());
            }
        });
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
                    msAvailableTrackLogs.loadFromBB(bBox);
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

    void newBB(){
        hideBB();
        bbcl = new BBControlLayer();
        register(bbcl, false); // the false prevents register to msLayers
    }

    public BBox getBBox(){
        return new BBox().extend(p1).extend(p2);
    }

    public void loadFromBB(){
        if ((p1!= null) && (p2!= null)){
            BBox bBox = new BBox().extend(p1).extend(p2);
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " bBox="+bBox);
            msAvailableTrackLogs.loadFromBB(bBox);
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
        getTimer().removeCallbacks(ttHide);
    }

    boolean isLoadAllowed(){
        return ((p1 != null) && (p2 != null) && (bbcl!= null));
    }

    public Control[] getMenuBBControls(){
        return new Control[]{
                new LoadBBControl(this),
                new LoadTSFromBBControl(getActivity(), getApplication(), this, false, false),
                new LoadTSFromBBControl(getActivity(), getApplication(), this, true, false),
                new LoadTSFromBBControl(getActivity(), getApplication(), this, true, true)
        };

    }

}
