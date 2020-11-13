/*
 * Copyright 2020 mg4gh
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
package mg.mapviewer;

import android.content.Context;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableRow;
import android.widget.TextView;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TileLayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;

import mg.mapviewer.control.CenterControl;
import mg.mapviewer.control.GpsControl;
import mg.mapviewer.control.HeightProfileControl;
import mg.mapviewer.control.SettingsControl;
import mg.mapviewer.control.ThemeSettingsControl;
import mg.mapviewer.control.TrackStatisticControl;
import mg.mapviewer.features.atl.MSAvailableTrackLogs;
import mg.mapviewer.features.bb.MSBB;
import mg.mapviewer.features.marker.MSMarker;
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.rtl.MSRecordingTrackLog;
import mg.mapviewer.features.search.SearchView;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.ZoomOCL;

/**
 * The control view is the parent container view object. So it is the parent for
 * <ul>
 *     <li>the dashboard</li>
 *     <li>all menu und submenu buttons</li>
 *     <li>the transparency control sliders</li>
 *     <li>the progress bar</li>
 *     <li>the status line</li>
 *     <li>the quick controls</li>
 * </ul>
 * @see <a href="../../../images/MGMapViewer_ViewModel.PNG">MGMapActivity_ViewModel</a>
 *
 */
public class ControlView extends RelativeLayout {

    Context context;
    MGMapApplication application = null;
    MGMapActivity activity = null;

    /** There are five dashboard entries, each with a specific semantic. This ordered list contains the viewIds of these five entries. */
    ArrayList<Integer> dashboardKeys = new ArrayList<>();
    /** Mapping of the viewIDs to the Dashboard entries. The Dashboard entry objects are kept here even if they are not visible. */
    Map<Integer,ViewGroup> dashboardMap = new HashMap<>();
    /** Parent of the dashboard entries. */
    ViewGroup dashboard;
    /** Reference to the MapView object - some controls change properties of the mapView */
    MapView mapView = null;

    /** progress bar - only used in case of route opimization */
    ProgressBar progressBar = null;


    /** parent object for status line */
    TableRow tr_states = null;
    TextView tv_center = null;
    TextView tv_zoom = null;
    TextView tv_bat = null;
    TextView tv_time = null;
    TextView tv_height = null;
    public TextView tv_remain = null;
    ArrayList<TextView> tvList = new ArrayList<>();

    /** A Control object is an extension of a ViewOnClickListener. With this map it's easy to determine from the OnClickListener the corresponding View object. Furthermore this map provides a list of all menu and submenu views. */
    Map<Control, View> controlMap = new HashMap<>();

    public ControlView(Context context) {
        super(context);
        this.context = context;
    }

    public ControlView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;

    }

    public void init(final MGMapApplication application, final MGMapActivity activity){
        try {
            this.application = application;
            this.activity = activity;

            mapView = activity.getMapsforgeMapView();
            // this is necessary to get the scalbar above the quick controls and the status line
            mapView.getMapScaleBar().setMarginVertical((int)(60 * mapView.getModel().displayModel.getScaleFactor()));

            // initialize the dashboardKeys and dashboardMap object and then hide dashboard entries
            dashboard = findViewById(R.id.dashboard);
            dashboardKeys.clear();
            while (dashboard.getChildCount() > 0){
                ViewGroup entry = (ViewGroup) dashboard.getChildAt(0);
                dashboardKeys.add(entry.getId());
                dashboardMap.put(entry.getId(),entry);
                for (int i=0; i<entry.getChildCount(); i++){
                    entry.getChildAt(i).setOnClickListener(new ZoomOCL(mapView.getModel().displayModel.getScaleFactor()));
                }
                dashboard.removeViewAt(0);
            }

            prepareMenuOCLs();

            prepareAlphaSliders();

            prepareStatusLine();

            prepareHotControls();

        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context()+"", e);
        }
    }


    void registerControl(View view, Control control){
        control.setControlView(this);
        controlMap.put(control, view);
        view.setOnClickListener(control);
    }

    void registerSubmenu(ViewGroup menu, Control[] controls){
        if (menu.getChildCount() < controls.length){
            Log.e(MGMapApplication.LABEL, NameUtil.context() + " too much controls("+controls.length+") for menu("+menu.getChildCount()+")");
        }
        for (int idx=menu.getChildCount()-1; idx >= 0; idx--){
            if (idx < controls.length){
                registerControl(menu.getChildAt(idx), controls[idx]);
            } else {
                menu.removeViewAt(idx);
            }
        }
    }

    void registerMenuControl(View view, final String name, final ViewGroup menu, Control[] controls){
        registerSubmenu(menu, controls);
        Control control = new Control(false){
            @Override
            public void onClick(View v) {
                super.onClick(v);
                disableMainMenuButtons();
                showMenu(menu);
            }

            @Override
            public void onPrepare(View v) {
                setText(v, name);
            }
        };
        registerControl(view, control);
    }

    void disableMainMenuButtons(){
        for (int idx=0; idx<getChildCount(); idx++) {
            View chview = getChildAt(idx);
            if (chview instanceof Button) {
                chview.setEnabled(false); // disable mainmenu entries by default
            }
        }
    }

    public void showMenu(ViewGroup parent){
        for (Control control : controlMap.keySet()){
            View view = controlMap.get(control);
            if (view.getParent() == parent){
                view.setEnabled( true ); // default is enabled, might be reset in onPrepare
                control.onPrepare(view);
                view.setVisibility( VISIBLE );
            }
        }
    }

    public void hideMenu(){
        menuVisibility = false;
        for (Control control : controlMap.keySet()){
            View view = controlMap.get(control);

            if (view.getVisibility() == VISIBLE){
                view.setVisibility( INVISIBLE );
            }
        }
    }

    public MGMapActivity getActivity(){
        return activity;
    }
    public MGMapApplication getApplication(){
        return application;
    }



    public void prepareMenuOCLs(){


        registerControl(findViewById(R.id.bt_st01), new SettingsControl());
        registerControl(findViewById(R.id.bt_st02), new ThemeSettingsControl());
        registerControl(findViewById(R.id.bt_st03), new TrackStatisticControl());
        registerControl(findViewById(R.id.bt_st04), new HeightProfileControl());
        registerControl(findViewById(R.id.bt_st05), new CenterControl());
        registerControl(findViewById(R.id.bt_st06), new GpsControl());


        registerMenuControl(findViewById(R.id.bt_en01), rstring(R.string.btRecordTrack), (ViewGroup) findViewById(R.id.menu_en01_sub), getActivity().getMS(MSRecordingTrackLog.class).getMenuTrackControls());
        registerMenuControl(findViewById(R.id.bt_en02), rstring(R.string.btBB), (ViewGroup) findViewById(R.id.menu_en02_sub), getActivity().getMS(MSBB.class).getMenuBBControls());
        registerMenuControl(findViewById(R.id.bt_en03), rstring(R.string.btLoadTrack), (ViewGroup) findViewById(R.id.menu_en03_sub), getActivity().getMS(MSAvailableTrackLogs.class).getMenuLoadControls());
        registerMenuControl(findViewById(R.id.bt_en04), rstring(R.string.btHideTrack), (ViewGroup) findViewById(R.id.menu_en04_sub), getActivity().getMS(MSAvailableTrackLogs.class).getMenuHideControls());
        registerMenuControl(findViewById(R.id.bt_en05), rstring(R.string.btMarkerTrack), (ViewGroup) findViewById(R.id.menu_en05_sub), getActivity().getMS(MSMarker.class).getMenuMarkerControls());
        registerMenuControl(findViewById(R.id.bt_en06), rstring(R.string.btRoute), (ViewGroup) findViewById(R.id.menu_en06_sub), getActivity().getMS(MSRouting.class).getMenuRouteControls());
        hideMenu();

    }





    public String rstring(int id){
        return getResources().getString(id);
    }


    boolean menuVisibility = false;

    public void toggleMenuVisibility(){
        menuVisibility = !menuVisibility;
        if (menuVisibility) {
            showMenu(this);
            startTTHideButtons(7000);
        } else {
            hideMenu();
        }
    }


    private Handler timer = new Handler();
    final Runnable timerTaskHideButtons = new Runnable() {
        @Override
        public void run() {
            hideMenu();
            timer.removeCallbacks(timerTaskHideButtons);
        }
    };
    public void cancelTTHideButtons(){
        timer.removeCallbacks(timerTaskHideButtons);
    }
    public void startTTHideButtons(long millis){
        timer.removeCallbacks(timerTaskHideButtons);
        timer.postDelayed(timerTaskHideButtons, millis);
    }






    public void setDashboardVisibility(boolean visibitity){
        dashboard.setVisibility(visibitity?VISIBLE:INVISIBLE);
    }

    int getIndexForDashoardEntry(int dashboardId){
        int idx = 0;
        for (int key : dashboardKeys){
            if (key == dashboardId){
                return idx;
            } else {
                if (dashboardMap.get(key).getParent() != null){
                    idx++;
                }
            }
        }
        RuntimeException t = new RuntimeException("unknown Dashboard ID "+dashboardId);
        Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        return -1;
    }

    public void showHideUpdateDashboard(boolean condition, int dashboardId, TrackLogStatistic statistic) {
        ViewGroup dashboardEntry = dashboardMap.get(dashboardId);
        if (dashboardEntry == null) return;

        if(condition && (statistic != null)){
            TextView t0 = (TextView) dashboardEntry.getChildAt(0);
            String sIdx = "I="+statistic.getSegmentIdx();
            if (statistic.getSegmentIdx() == -1) sIdx = "All";
            if (statistic.getSegmentIdx() == -2) sIdx = "R";
            t0.setText(sIdx);
            TextView t1 = (TextView) dashboardEntry.getChildAt(1);
            t1.setText(String.format(Locale.ENGLISH, "%.2fkm", statistic.getTotalLength()/1000));
            TextView t2 = (TextView) dashboardEntry.getChildAt(2);
            t2.setText(String.format(Locale.ENGLISH, "%.1fm", statistic.getGain()));
            TextView t3 = (TextView) dashboardEntry.getChildAt(3);
            t3.setText(String.format(Locale.ENGLISH, "%.1fm", statistic.getLoss()));
            TextView t4 = (TextView) dashboardEntry.getChildAt(4);
            t4.setText(String.format(Locale.ENGLISH, "%s", statistic.durationToString()));
            if (dashboardEntry.getParent() == null){
                dashboard.addView(dashboardEntry,getIndexForDashoardEntry(dashboardId));
            }

        } else {
            if (dashboardEntry.getParent() != null) {
                dashboard.removeView(dashboardEntry);
            }
        }
    }

    Map<String, SeekBar> mapSliders = new HashMap<>();
    Map<String, TextView> mapSliderNames = new HashMap<>();
    private void prepareAlphaSliders(){
        mapSliders.put(context.getResources().getString( R.string.preference_choose_map_key1 ), (SeekBar) findViewById(R.id.sb1));
        mapSliders.put(context.getResources().getString( R.string.preference_choose_map_key2 ), (SeekBar) findViewById(R.id.sb2));
        mapSliders.put(context.getResources().getString( R.string.preference_choose_map_key3 ), (SeekBar) findViewById(R.id.sb3));
        mapSliders.put(context.getResources().getString( R.string.preference_choose_map_key4 ), (SeekBar) findViewById(R.id.sb4));
        mapSliders.put(context.getResources().getString( R.string.preference_choose_map_key5 ), (SeekBar) findViewById(R.id.sb5));

        mapSliderNames.put(context.getResources().getString( R.string.preference_choose_map_key1 ), (TextView) findViewById(R.id.sbt1));
        mapSliderNames.put(context.getResources().getString( R.string.preference_choose_map_key2 ), (TextView) findViewById(R.id.sbt2));
        mapSliderNames.put(context.getResources().getString( R.string.preference_choose_map_key3 ), (TextView) findViewById(R.id.sbt3));
        mapSliderNames.put(context.getResources().getString( R.string.preference_choose_map_key4 ), (TextView) findViewById(R.id.sbt4));
        mapSliderNames.put(context.getResources().getString( R.string.preference_choose_map_key5 ), (TextView) findViewById(R.id.sbt5));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        LinearLayout parentLayer = findViewById(R.id.bars);
        for (String prefKey : application.getMapLayerKeys()) {
            final String key = sharedPreferences.getString(prefKey, "");
            final String alphakey = "no_recreate_alpha_"+key;
            Layer layer = MGMapLayerFactory.getMapLayer(key);
            boolean providesAlpha = (layer instanceof TileLayer);

            SeekBar seekBar = mapSliders.get(prefKey);
            seekBar.setVisibility(providesAlpha?VISIBLE:INVISIBLE);
            TextView seekBarName = mapSliderNames.get(prefKey);
            seekBarName.setText(key);
            seekBarName.setVisibility(providesAlpha?VISIBLE:INVISIBLE);
            if (layer instanceof TileLayer) {
                TileLayer tileLayer = (TileLayer) layer;
                float alpha = tileLayer.getAlpha();
                alpha = sharedPreferences.getFloat(alphakey, alpha);
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+alphakey+" "+alpha+ " "+seekBar);
                tileLayer.setAlpha(alpha);
                seekBar.setProgress((int) (alpha*100));

                seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        Layer layer = MGMapLayerFactory.getMapLayer(key);
                        if (layer instanceof TileLayer) {
                            TileLayer tileLayer = (TileLayer) layer;
                            float alpha = progress/100.0f;
                            if (!fromUser){
                                alpha = sharedPreferences.getFloat(alphakey, alpha);
                                seekBar.setProgress((int) (alpha*100));
                            }
                            tileLayer.setAlpha(alpha);
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+alphakey+" "+alpha+ " "+seekBar);
                            sharedPreferences.edit().putFloat(alphakey, alpha).apply();
                            tileLayer.setVisible(progress != 0);
                            mapView.getLayerManager().redrawLayers();
                        }
                        Log.d(MGMapApplication.LABEL, NameUtil.context()+" progress="+progress);
                    }
                });

            } else {
                parentLayer.removeView(seekBar);
                parentLayer.removeView(seekBarName);
            }


        }
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " showAlphaSliders="+application.showAlphaSliders.getValue());
        application.showAlphaSliders.changed();
    }



    TextView registerTextView(int id){
        TextView tv = activity.findViewById(id);
        tvList.add(tv);
        tv.setOnClickListener(new ZoomOCL(mapView.getModel().displayModel.getScaleFactor()));
        return tv;
    }



    void prepareStatusLine(){
        tr_states = activity.findViewById(R.id.tr_states);

        tv_center = registerTextView(R.id.tv_center);
        tv_zoom = registerTextView(R.id.tv_zoom);
        tv_time = registerTextView(R.id.tv_time);
        tv_height = registerTextView(R.id.tv_height);
        tv_remain = registerTextView(R.id.tv_remain);
        tv_bat = registerTextView(R.id.tv_bat);

        updateTvCenter(0);
        updateTvTime(System.currentTimeMillis());
        updateTvHeight(PointModel.NO_ELE);
        updateRemainingForSelected(0, -1);

    }

    public void setTvText(TextView textView, String text){
        textView.setText(text);
        if ((textView.getParent() == null) && (text != null) && (!("".equals(text)))){ // ok, add the view
            //determine first the position to insert the view
            int cntVisible = 0;
            for (TextView tv : tvList){
                if (tv == textView) break;
                if (tv.getParent() != null) cntVisible++;
            }
            if (cntVisible < tvList.indexOf(tv_bat)){ // don't add tv_bat, if all other tv's are visible
                tr_states.addView(textView, cntVisible);
            }

            if (tv_bat.getParent() != null){ // check, whether tv_bat has to be removed
                //determine, how much tv's are visible after adding
                cntVisible = 0;
                for (TextView tv : tvList){
                    if (tv.getParent() != null) cntVisible++;
                }
                if (cntVisible > tvList.indexOf(tv_bat)){  //are all other tv's before tv_bat also visible
                    tr_states.removeView(tv_bat);
                }
            }
        } else if ((textView.getParent() != null) && (("".equals(text)) || (text == null)) ){ // opposite, remove a view
            tr_states.removeView(textView);

            if (tv_bat.getParent() == null){ // check, whether tv_bat has to be added
                //determine, how much tv's are visible after adding
                int cntVisible = 0;
                for (TextView tv : tvList){
                    if (tv.getParent() != null) cntVisible++;
                }
                if (cntVisible < tvList.indexOf(tv_bat)){  //not all other tv's before tv_bat are visible
                    tr_states.addView(tv_bat);
                }
            }
        }
    }



    public void updateTvCenter(double distance){ // distance 0 means clean
        String text = (distance==0)?"":String.format(Locale.ENGLISH, " %.2f km", distance / 1000.0);
        setTvText(tv_center, text);
    }

    public void updateTvZoom(byte zoomLevel){ // zoomLevel 0 means clean
        String text = (zoomLevel==0)?"":String.format(Locale.ENGLISH, " %d", zoomLevel);
        setTvText(tv_zoom, text);
    }

    private SimpleDateFormat sdf2 = new SimpleDateFormat(" HH:mm", Locale.GERMANY);
    public void updateTvTime(long millis){
        String text = (millis==0)?"": sdf2.format(millis);
        setTvText(tv_time, text);
    }

    public void updateTvHeight(float height /* in m */){ // PointModel.NO_ELE means clean
        String text = (height == PointModel.NO_ELE)?"":String.format(Locale.ENGLISH," %.1f m",height);
        setTvText(tv_height, text);
    }


    public void updateRemainingForSelected(int drawableId, double dist){
        String text = (dist<0)?"":String.format(Locale.ENGLISH, " %.2f km", dist/1000);

        Rect rect = new Rect(0,0,40,40);
        try {
            rect = tv_remain.getCompoundDrawables()[0].getBounds(); // if available, take these values
        } catch (Exception e) {}
        Drawable drawable = null;
        if (drawableId != 0){
            drawable = context.getResources().getDrawable( drawableId, context.getTheme());
            drawable.setBounds(rect);
        }
        tv_remain.setCompoundDrawables(drawable,null,null,null);
        setTvText(tv_remain, text);

    }

    public void updateTvBat(int batteryPercent){ // batteryPercent -1 means clean
        String text = (batteryPercent==-1)?"":String.format(Locale.ENGLISH, " %d", batteryPercent);
        setTvText(tv_bat, text);
    }


    private void scaleBoundsForDrawable(TextView tv, int diff){
        Drawable drawable = tv.getCompoundDrawables()[0]; // index 0 is the left
        if (drawable != null){
            Rect bounds = drawable.getBounds();
            bounds.right += diff;
            bounds.bottom += diff;
            drawable.setBounds(bounds);
            tv.setCompoundDrawables(drawable,null,null,null);
        }
    }


    private void prepareHotControls(){

        final TextView ct1 = activity.findViewById(R.id.ct1);
        ct1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                application.fullscreen.toggle();
            }
        });
        final TextView ct2 = activity.findViewById(R.id.ct2);
        ct2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                application.showAlphaSliders.toggle();
            }
        });
        application.showAlphaSliders.deleteObservers(); // if there are old observers
        application.showAlphaSliders.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                findViewById(R.id.bars).setVisibility( application.showAlphaSliders.getValue()?VISIBLE:INVISIBLE );
            }
        });
        application.showAlphaSliders.changed();

        final TextView ct3 = activity.findViewById(R.id.ct3);
        ct3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                application.editMarkerTrack.toggle();
            }
        });
        ct3.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                application.routingHints.toggle();
                return true;
            }
        });
        Observer ct3Observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Rect rect = ct3.getCompoundDrawables()[0].getBounds();
                int id = application.editMarkerTrack.getValue()?(application.routingHints.getValue()? R.drawable.mtlr4:R.drawable.mtlr2):(application.routingHints.getValue()? R.drawable.mtlr3:R.drawable.mtlr);
                Drawable drawable = context.getResources().getDrawable(id, context.getTheme());
                drawable.setBounds(rect.left,rect.top,rect.right,rect.bottom);
                ct3.setCompoundDrawables(drawable,null,null,null);
            }
        };
        application.editMarkerTrack.addObserver(ct3Observer);
        application.routingHints.addObserver(ct3Observer);

        final TextView ct3a = activity.findViewById(R.id.ct3a);
        ct3a.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                application.bboxOn.toggle();
            }
        });
        ct3a.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                application.bboxOn.setValue(true);
                application.getMS(MSBB.class).initFromScreen = true;
                application.getMS(MSBB.class).triggerRefresh();
                return true;
            }
        });
        Observer ct3aObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Rect rect = ct3a.getCompoundDrawables()[0].getBounds();
                int id = application.bboxOn.getValue()?R.drawable.bbox2:R.drawable.bbox;
                Drawable drawable = context.getResources().getDrawable(id, context.getTheme());
                drawable.setBounds(rect.left,rect.top,rect.right,rect.bottom);
                ct3a.setCompoundDrawables(drawable,null,null,null);
            }
        };
        application.bboxOn.addObserver(ct3aObserver);


        final TextView ct3b = activity.findViewById(R.id.ct3b);
        ct3b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                application.searchOn.toggle();
            }
        });


        final TextView ct4 = activity.findViewById(R.id.ct4);
        ct4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.getModel().mapViewPosition.zoomIn();
            }
        });
        final TextView ct5 = activity.findViewById(R.id.ct5);
        ct5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.getModel().mapViewPosition.zoomOut();
            }
        });

    }
}