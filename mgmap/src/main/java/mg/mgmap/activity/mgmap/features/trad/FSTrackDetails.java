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

import org.mapsforge.map.layer.Layer;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;

import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.VUtil;

public class FSTrackDetails extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    private static final int tdHeight = VUtil.dp(50);

    private final RelativeLayout trackDetailsView;

    private boolean tdVisibility = false;
    private String tdDashboardId = null;
    private int tdColorId = 0;
    private TrackLog tdTrackLog = null;
    private Observable tdObservable = null;
    private int tdDrawableId = 0;

    private final TdMarker tdm1;
    private final TdMarker tdm2;
    private TradControlLayer tdControlLayer = null;



    public class TradControlLayer extends ControlMVLayer<TdMarker> {

        Point dragOffset = null;
        @Override
        protected boolean checkDrag(float scrollX, float scrollY) {
            if ((dragOffset=markerDragMatch(scrollX,scrollY,tdm1.getMarkerRect()))!=null){
                setDragObject(tdm1);
                return true;
            } else if ((dragOffset=markerDragMatch(scrollX,scrollY,tdm2.getMarkerRect()))!=null){
                setDragObject(tdm2);
                return true;
            }
            return false;
        }

        @Override
        protected void handleDrag(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
            WriteablePointModel pmCurrent = new WriteablePointModelImpl(y2lat(scrollY2+dragOffset.y), x2lon(scrollX2+dragOffset.x));
            getDragObject().setPoint(pmCurrent);

 //           super.handleDrag(scrollX1, scrollY1, scrollX2, scrollY2);
        }

        @Override
        protected void handleDrag(WriteablePointModel pmCurrent) {
            getDragObject().setPoint(pmCurrent);

//            super.handleDrag(pmCurrent);
        }
    }

    public Point markerDragMatch(float x, float y, Rect rect){
        if (rect.contains((int)x,(int)y)){
            if (y <= (2* (rect.bottom - rect.top)) / 3f + rect.top){ // match 2/3 at top of rect
                int dx6 = (rect.right - rect.left) /6;
                if ((rect.left+dx6 <= x) && (x <= rect.right-dx6)){
                    return new Point(rect.centerX()-(int)x, rect.bottom-(int)y);
                }
            }
        }
        return null;
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
        refreshObserver.onChange();
        if (tdDashboardId != null) {
            registerTDService();
        }
    }

    @Override
    protected void onPause() {
        if (tdDashboardId != null){
            unregisterTDService();
        }
    }

    @Override
    protected void doRefreshResumedUI() {
        tdm1.refresh();
        tdm2.refresh();
    }


    private void setVisibility(boolean visible){
        if (tdVisibility != visible){
            tdVisibility = visible;

        }
    }

    /** return true if drag should be aborted */
    public boolean handleDashboardDrag( String id, float startX, float startY, float currentX, float currentY){
        boolean rc = false;
        float dy = currentY - startY;
        if (tdVisibility){
            if (dy < 0){
                if (dy < -tdHeight){
                    setTrackDetailsHeight(0);
                    setVisibility(false);
                    rc = true;
                    setDashboardId(null);
                } else {
                    setTrackDetailsHeight(tdHeight + (int)dy);
                }
            } else {
                rc = true;
            }
        } else {
            if (dy > 0){
                setDashboardId(id);
                if (dy > tdHeight){
                    setTrackDetailsHeight(tdHeight);
                    setVisibility(true);
                    rc = true;
                } else {
//                    mgLog.d("height="+((int)dy) );
                    setTrackDetailsHeight( (int) dy);
                }
            }else {
                rc = true;
            }
        }
        return rc;
    }

    public void abortDashboardDrag(){
        if (trackDetailsView.getHeight() > tdHeight/2){
            setTrackDetailsHeight(tdHeight);
            setVisibility(true);
        } else {
            setTrackDetailsHeight( 0 );
            setDashboardId(null);
            setVisibility(false);
        }
    }


    private void setTrackDetailsHeight(int height){
        trackDetailsView.getLayoutParams().height = height;
        trackDetailsView.setLayoutParams(trackDetailsView.getLayoutParams());

    }

    private void setDashboardId(String id){
        if ((tdDashboardId == null) && (id != null)){
            tdDashboardId = id;
            switch (id){
                case "rtl":
                case "rtls":
                    tdColorId = R.color.CC_RED100_A100;
                    tdTrackLog = getApplication().recordingTrackLogObservable.getTrackLog();
                    tdObservable = getApplication().recordingTrackLogObservable;
                    tdDrawableId = R.drawable.td_marker_rtl;
                    break;
                case "route":
                    tdColorId = R.color.CC_PURPLE_A100;
                    tdTrackLog = getApplication().routeTrackLogObservable.getTrackLog();
                    tdObservable = getApplication().routeTrackLogObservable;
                    tdDrawableId = R.drawable.td_marker_route;
                    break;
                case "stl":
                case "stls":
                    tdColorId = R.color.CC_BLUE100_A100;
                    tdTrackLog = getApplication().availableTrackLogsObservable.getSelectedTrackLogRef().getTrackLog();
                    tdObservable = getApplication().availableTrackLogsObservable;
                    tdDrawableId = R.drawable.td_marker_stl;
                    break;
                default:
                    mgLog.e("unexpected id value: "+id);
            }
            registerTDService();
        } else if ((tdDashboardId != null) && (id == null)) {
            unregisterTDService();
            tdDashboardId = null;
            tdColorId = 0;
            tdTrackLog = null;
            tdObservable = null;
            tdDrawableId = 0;
        }
    }

    private void registerTDService(){
        trackDetailsView.setBackgroundColor(CC.getColor(tdColorId));
        tdObservable.addObserver(refreshObserver);
        tdControlLayer = new TradControlLayer();
        register(tdControlLayer);

        Drawable drawableBg = ResourcesCompat.getDrawable(getResources(), R.drawable.td_marker_bg, getActivity().getTheme());
        Drawable drawableFg = ResourcesCompat.getDrawable(getResources(), tdDrawableId, getActivity().getTheme());

        tdm1.setDrawable(drawableBg, drawableFg);
        tdm2.setDrawable(drawableBg, drawableFg);
    }
    private void unregisterTDService(){
        trackDetailsView.setBackgroundColor(CC.getColor(tdColorId));
        tdObservable.deleteObserver(refreshObserver);
        unregisterAllControl();
        unregisterAll();
    }

    @Override
    protected void unregisterAll() {
        super.unregisterAll();
    }

    @Override
    protected void register(Layer layer) {
        super.register(layer);
    }

    @Override
    protected void unregister(Layer layer) {
        super.unregister(layer);
    }

    @Override
    protected void redraw() {
        super.redraw();
    }
}
