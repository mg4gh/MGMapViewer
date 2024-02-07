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
package mg.mgmap.activity.mgmap.features.trad;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.widget.RelativeLayout;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.core.model.LatLong;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.VUtil;

public class FSTrackDetails extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    private static final int tdHeight = VUtil.dp(60);

    private final RelativeLayout trackDetailsView;

    private boolean tdVisibility = false;
    private String tdDashboardId = null;
    private int tdColorId = 0;
    private Observable tdObservable = null;
    private int tdDrawableId = 0;

    private final TdMarker tdm1;
    private final TdMarker tdm2;
    private TradControlLayer tdControlLayer = null;



    private boolean visibilityAtDashboardDragStart = false;

    public class TradControlLayer extends ControlMVLayer<TdMarker> {

        Point dragOffset = null;

        @Override
        public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY, org.mapsforge.core.model.Point tapXY) {
            if (markerDragMatch((float) tapXY.x, (float) tapXY.y, tdm1.getMarkerRect(true)) != null) {
                getMapViewUtility().setCenter(tdm1.getPosition());
            } else if (markerDragMatch((float) tapXY.x, (float) tapXY.y, tdm2.getMarkerRect(true)) != null) {
                getMapViewUtility().setCenter(tdm2.getPosition());
            } else {
                return false;
            }
            return true;
        }

        @Override
        protected boolean checkDrag(float scrollX, float scrollY) {
            if ((dragOffset=markerDragMatch(scrollX,scrollY,tdm1.getMarkerRect(false)))!=null){
                setDragObject(tdm1);
                return true;
            } else if ((dragOffset=markerDragMatch(scrollX,scrollY,tdm2.getMarkerRect(false)))!=null){
                setDragObject(tdm2);
                return true;
            }
            return false;
        }

        @Override
        protected void handleDrag(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
            WriteablePointModel pmCurrent = new WriteablePointModelImpl(y2lat(scrollY2+dragOffset.y), x2lon(scrollX2+dragOffset.x));
            getDragObject().setPosition(pmCurrent);
            triggerRefreshObserver();
        }

    }



    public FSTrackDetails(MGMapActivity mmActivity) {
        super(mmActivity);
        trackDetailsView = mmActivity.findViewById(R.id.trackDetails);
        trackDetailsView.setBackgroundColor(0x00000000);

        int totalWidth = getResources().getDisplayMetrics().widthPixels;
        tdm1 = new TdMarker(this, trackDetailsView, true, totalWidth, tdHeight);
        tdm2 = new TdMarker(this, trackDetailsView, false, totalWidth, tdHeight);
    }

    @Override
    protected void onResume() {
        triggerRefreshObserver();
        if (tdVisibility) {
            registerTDService();
        }
    }

    @Override
    protected void onPause() {
        if (tdVisibility){
            unregisterTDService();
        }
    }

    @Override
    protected void redraw() {
        super.redraw();
    }

    void triggerRefreshObserver(){
        refreshObserver.onChange();
    }

    @Override
    protected void doRefreshResumedUI() {
        if (tdVisibility){
            TrackLog trackLog = null;
            if (tdObservable instanceof MGMapApplication.TrackLogObservable) {
                trackLog = ((MGMapApplication.TrackLogObservable<?>) tdObservable).getTrackLog();
            } else if (tdObservable instanceof MGMapApplication.AvailableTrackLogsObservable) {
                trackLog = ((MGMapApplication.AvailableTrackLogsObservable) tdObservable).getSelectedTrackLogRef().getTrackLog();
            }
            if (trackLog == null){
                tdDashboardId = "invalid"; // reopen of track details will reset also points
                setVisibilityAndHeight(false, 0);
            } else if (tdVisibility){ // if visibility is still given
                TrackLogRefApproach approach1 = verifyPosition(tdm1, trackLog);
                TrackLogRefApproach approach2 = verifyPosition(tdm2, trackLog);
                TrackLogStatistic statistic = new TrackLogStatistic();
                trackLog.subStatistic(statistic,approach1,approach2);
                getControlView().setDashboardValue(tdDashboardId, statistic);
            }
        }
    }


    TrackLogRefApproach verifyPosition(TdMarker tdm, TrackLog trackLog){
        PointModel tdmPm = tdm.getPosition();
        TrackLogRefApproach approach = null;
        if (tdm.getPosition().getLaLo() != PointModelUtil.NO_POS){
            getTimer().removeCallbacks(tdm.ttPositionTimeout);
            approach = trackLog.getBestDistance(tdm.getPosition(), getMapViewUtility().getCloseThresholdForZoomLevel());
            if (approach != null){
                tdm.setPosition(approach.getApproachPoint());
            } else {
                tdm.setPosition(tdmPm); // just refresh
                getTimer().postDelayed(tdm.ttPositionTimeout,3000);
            }
        }
        return approach;
    }

    private void setVisibilityAndHeight(boolean visibility, int height){
        if (tdVisibility != visibility){
            tdVisibility = visibility;
            if (tdVisibility){
                registerTDService();
                triggerRefreshObserver();
            } else {
                unregisterTDService();
            }
        }
        mgLog.i("set height="+height);
        trackDetailsView.getLayoutParams().height = height;
        trackDetailsView.setLayoutParams(trackDetailsView.getLayoutParams());
        mgLog.i("get height="+trackDetailsView.getHeight()+ " "+trackDetailsView.getLayoutParams().height);
    }

    public void initDashboardDrag(String id){
        visibilityAtDashboardDragStart = tdVisibility;
        if (id != null){ // should always be true
            if (!tdVisibility){
                setDashboardId(id);
                setVisibilityAndHeight(true,0);
            }
        }
    }
    /** return true if drag should be aborted */
    public boolean handleDashboardDrag( float startX, float startY, float currentX, float currentY){
        boolean abort = false;
        float dx = currentX - startX;
        float dy = currentY - startY;
        if (Math.abs(dx) > Math.abs(dy)){
            abort = true;
        } else {
            if (visibilityAtDashboardDragStart){
                if (dy <= 0){
                    if (dy <= -tdHeight){
                        setVisibilityAndHeight(false, 0);
                        abort = true;
                    } else {
                        setVisibilityAndHeight(true,tdHeight + (int)dy);
                    }
                } else {
                    abort = true;
                }
            } else {
                if (dy >= 0){
                    if (dy >= tdHeight){
                        setVisibilityAndHeight(true,tdHeight);
                        abort = true;
                    } else {
                        setVisibilityAndHeight(true, (int) dy);
                    }
                }else {
                    abort = true;
                }
            }
        }
        return abort;
    }

    public void abortDashboardDrag(){
        if (trackDetailsView.getLayoutParams().height > tdHeight/2){ // check requested height, since actual height might not be updated right now
            setVisibilityAndHeight(true, tdHeight);
        } else {
            setVisibilityAndHeight(false, 0);
        }
    }


    private void setDashboardId(String id){
        if (!id.equals(tdDashboardId)){
            tdDashboardId = id;
            switch (id){
                case "rtl":
                case "rtls":
                    tdColorId = R.color.CC_RED100_A100;
                    tdObservable = getApplication().recordingTrackLogObservable;
                    tdDrawableId = R.drawable.td_marker_rtl;
                    break;
                case "route":
                    tdColorId = R.color.CC_PURPLE_A100;
                    tdObservable = getApplication().routeTrackLogObservable;
                    tdDrawableId = R.drawable.td_marker_route;
                    break;
                case "stl":
                case "stls":
                    tdColorId = R.color.CC_BLUE100_A100;
                    tdObservable = getApplication().availableTrackLogsObservable;
                    tdDrawableId = R.drawable.td_marker_stl;
                    break;
                default:
                    mgLog.e("unexpected id value: "+id);
            }
            Drawable drawableBg = ResourcesCompat.getDrawable(getResources(), R.drawable.td_marker_bg, getActivity().getTheme());
            Drawable drawableFg = ResourcesCompat.getDrawable(getResources(), tdDrawableId, getActivity().getTheme());
            tdm1.setDrawable(drawableBg, drawableFg);
            tdm2.setDrawable(drawableBg, drawableFg);
            tdm1.resetPosition();
            tdm2.resetPosition();
        }
    }

    private void registerTDService(){
        trackDetailsView.setBackgroundColor(CC.getColor(tdColorId));
        tdObservable.addObserver(refreshObserver);
        tdControlLayer = new TradControlLayer();
        register(tdControlLayer);
        register(tdm1.registerTDService());
        register(tdm2.registerTDService());
    }
    private void unregisterTDService(){
        tdObservable.deleteObserver(refreshObserver);
        unregisterAllControl();
        unregisterAll();
        tdm1.unregisterTDService();
        tdm2.unregisterTDService();
        getControlView().setDashboardValue(tdDashboardId, null);
    }



    private Point markerDragMatch(float x, float y, Rect rect){
        if (((-1* (rect.bottom - rect.top)) / 3f + rect.top <= y) && (y <= (2* (rect.bottom - rect.top)) / 3f + rect.top)){ // match 2/3 at top of rect
            int dx6 = (rect.right - rect.left) /6;
            if ((rect.left+dx6 <= x) && (x <= rect.right-dx6)){
                return new Point(rect.centerX()-(int)x, rect.bottom-(int)y);
            }
        }
        return null;
    }

}
