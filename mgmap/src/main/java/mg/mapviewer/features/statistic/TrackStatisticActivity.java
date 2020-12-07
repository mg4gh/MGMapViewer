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
package mg.mapviewer.features.statistic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.StrictMode;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.ExtendedClickListener;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.GpxExporter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.view.PrefTextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class TrackStatisticActivity extends AppCompatActivity {

    private Context context = null;
    private MGMapApplication application = null;

    public final Set<String> nameKeys = new TreeSet<>();
    private SparseArray<TrackLog> existingTrackLogs = new SparseArray<>(); // maps viewId -> TrackLog
    private SparseArray<TrackLog> selectedTrackLogs = new SparseArray<>(); // maps viewId -> TrackLog (only for selected entries)
    private SparseArray<StatisticClickListener> statisticClickListeners = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_statistic_activity);
        application = (MGMapApplication)getApplication();
        context = application.getApplicationContext();

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }


    boolean working = false;

    @Override
    protected void onStart() {
        super.onStart();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout parent = findViewById(R.id.trackStatisticEntries);



        addTrackLog(parent, application.recordingTrackLogObservable.getTrackLog(), R.color.RED100_A100, R.color.RED100_A150);
        addTrackLog(parent, application.markerTrackLogObservable.getTrackLog(), R.color.PINK_A100, R.color.PINK_A150);
        addTrackLog(parent, application.routeTrackLogObservable.getTrackLog(), R.color.PURPLE_A100, R.color.PURPLE_A150);
        addTrackLog(parent, application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog(), R.color.BLUE100_A100, R.color.BLUE150_A150);
        for (TrackLog trackLog : application.availableTrackLogsObservable.availableTrackLogs){
            addTrackLog(parent, trackLog, R.color.GREEN100_A100, R.color.GREEN150_A150);
        }
        ArrayList<TrackLog> trackLogsRemain = new ArrayList<>(application.metaTrackLogs.values());

        new Thread(){
            @Override
            public void run() {
                while (!trackLogsRemain.isEmpty()){
                    try {
                        Log.v(MGMapApplication.LABEL, NameUtil.context()+" remainA working="+working+" size="+trackLogsRemain.size());
                        if (!working){
                            working = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(MGMapApplication.LABEL, NameUtil.context()+" remainB working="+working+" size="+trackLogsRemain.size());
                                    for (int i=0; (i<30)&&(!trackLogsRemain.isEmpty()) ; i++){
                                        TrackLog trackLog = trackLogsRemain.remove(0);
                                        addTrackLog(parent, trackLog, R.color.GRAY100_A100, R.color.GRAY100_A150);
                                    }
                                    Log.v(MGMapApplication.LABEL, NameUtil.context()+" remainC working="+working+" size="+trackLogsRemain.size());
                                    working = false;
                                }
                            });

                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private int convertDp(float dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public PrefTextView createPTV(ViewGroup vgDashboard, float weight) {
        PrefTextView ptv = new PrefTextView(application.getMgMapActivity()); // Need activity context for Theme.AppCompat (Otherwise we get error messages)
        vgDashboard.addView(ptv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = convertDp(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        ptv.setLayoutParams(params);

        int padding = convertDp(2.0f);
        ptv.setPadding(padding, padding, padding, padding);
        int drawablePadding = convertDp(3.0f);
        ptv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.quick2, context.getTheme());

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ptv.setCompoundDrawables(drawable,null,null,null);
        ptv.setText("");
        ptv.setTextColor(CC.getColor(R.color.WHITE));
        ptv.setLines(1);
        return ptv;
    }


    static Random random = new Random(System.currentTimeMillis());
    public void addTrackLog(ViewGroup parent, TrackLog trackLog, int colorId, int colorIdSelected){
        if (trackLog == null) return;
        if (nameKeys.contains(trackLog.getNameKey())) return; // this TrackLog is already added
        TrackLogStatistic statistic = trackLog.getTrackStatistic();
        if (statistic.getNumPoints() == 0) return; // don't show empty tracks, especially possible for marker Tracks
        nameKeys.add(trackLog.getNameKey());

        TableLayout tableLayout = new TableLayout(context);
        parent.addView(tableLayout);
        tableLayout.setId(View.generateViewId());
        tableLayout.setPadding(0, convertDp(2),0,0);
        existingTrackLogs.put(tableLayout.getId(), trackLog);
        registerForContextMenu(tableLayout);

        TableRow tableRow0 = new TableRow(context);
        tableLayout.addView(tableRow0);
        PrefTextView namePTV = createPTV(tableRow0,1).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(null,null);
        namePTV.setValue(trackLog.getName()+(trackLog.isModified()?"(*)":""));
        namePTV.setMaxLines(5);

        TableRow tableRow1 = new TableRow(context);
        tableLayout.addView(tableRow1);
        createPTV(tableRow1,10).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(null,null).setValue( (statistic.getSegmentIdx()<0)?"All":""+statistic.getSegmentIdx() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DATE).setPrefData(null,null).setValue( statistic.getTStart() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_TIME).setPrefData(null,null).setValue( statistic.getTStart() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DURATION).setPrefData(null, new int[]{R.drawable.duration}).setValue( statistic.duration );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setPrefData(null, new int[]{R.drawable.length}).setValue( statistic.getTotalLength() );

        TableRow tableRow2 = new TableRow(context);
        tableLayout.addView(tableRow2);
        createPTV(tableRow2,10).setFormat(Formatter.FormatType.FORMAT_INT).setPrefData(null,null).setValue( statistic.getNumPoints() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.gain}).setValue( statistic.getGain() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.loss}).setValue( statistic.getLoss() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.maxele}).setValue( statistic.getMaxEle() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.minele}).setValue( statistic.getMinEle() );


        setViewtreeColor(tableLayout, colorId);

        if (trackLog != application.recordingTrackLogObservable.getTrackLog()) {
            StatisticClickListener statisticClickListener = new StatisticClickListener(colorId,colorIdSelected,namePTV);
            tableLayout.setOnClickListener(new StatisticClickListener(colorId,colorIdSelected,namePTV));
            statisticClickListeners.put(tableLayout.getId(), statisticClickListener);
        }

    }

    public class StatisticClickListener extends ExtendedClickListener{
        private int colorId;
        private int colorIdSelected;
        private PrefTextView namePTV;
        public StatisticClickListener(int colorId, int colorIdSelected, PrefTextView namePTV){
            this.colorId = colorId;
            this.colorIdSelected = colorIdSelected;
            this.namePTV = namePTV;
        }
        @Override
        public void onSingleClick(View v) {
            int id = v.getId();
            if (selectedTrackLogs.get(id) == null){
                selectedTrackLogs.put(id, existingTrackLogs.get(id));
                setViewtreeColor(v, colorIdSelected);
            } else {
                selectedTrackLogs.remove(id);
                setViewtreeColor(v, colorId);
            }
        }

        private boolean sortName = false;
        @Override
        public void onDoubleClick(View v) {
            sortName = !sortName;
            TrackLog trackLog = existingTrackLogs.get(v.getId());
            if (sortName){
                namePTV.setText(trackLog.getNameKey());
            } else {
                namePTV.setText(trackLog.getName());
            }
            super.onDoubleClick(v);
        }

    }

    private void setViewtreeColor(View view, int colorId){
        if (view instanceof TextView){
            view.setBackgroundColor(getResources().getColor( colorId, getTheme()) );
        }

        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup)view;
            for (int idx=0; idx<viewGroup.getChildCount(); idx++){
                setViewtreeColor(viewGroup.getChildAt(idx), colorId);
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        LinearLayout parent = findViewById(R.id.trackStatisticEntries);
        parent.removeAllViews();
        existingTrackLogs.clear();
        selectedTrackLogs.clear();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("MGMapViewer:");
        if (v instanceof TableLayout){
            if (existingTrackLogs.get(v.getId()) != application.recordingTrackLogObservable.getTrackLog()){
                int id = v.getId();

                if (selectedTrackLogs.get(id) == null) {
                    StatisticClickListener scl = statisticClickListeners.get(id);
                    scl.onSingleClick(v);
                }

                menu.add(0, v.getId(),0,getResources().getString(R.string.ctx_stat_stl_track));
                boolean bModified = false;
                for(int idx = 0; idx < selectedTrackLogs.size(); idx++) {
                    TrackLog aTrackLog = selectedTrackLogs.valueAt(idx);
                    if (aTrackLog.isModified()){
                        bModified = true;
                    }
                }
                if (bModified){
                    menu.add(0, v.getId(),0,getResources().getString(R.string.ctx_stat_sav_track));
                }
                if (selectedTrackLogs.size() == 1){
                    TrackLog trackLog = selectedTrackLogs.get(v.getId());
                    boolean bProvideMarkerEntry = true;
                    if (application.recordingTrackLogObservable.getTrackLog() == trackLog) bProvideMarkerEntry = false;
                    if (application.markerTrackLogObservable.getTrackLog() == trackLog) bProvideMarkerEntry = false;
                    if (application.routeTrackLogObservable.getTrackLog() == trackLog) bProvideMarkerEntry = false;
                    if (bProvideMarkerEntry){
                        menu.add(0, v.getId(),0,getResources().getString(R.string.ctx_stat_mtl_track));
                    }
                }
                menu.add(0, v.getId(),0,getResources().getString(bModified?R.string.ctx_stat_shr_track2:R.string.ctx_stat_shr_track));
                if (!bModified){
                    menu.add(0, v.getId(),1,getResources().getString(R.string.ctx_stat_del_track));
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        TrackLog trackLog = existingTrackLogs.get(item.getItemId());
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" \""+item.getTitle()+"\" for track \"" + trackLog.getName()+"\"");

        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_del_track) )) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(item.getTitle());
            ArrayList<String> list = new ArrayList<>();
            for(int idx = 0; idx < selectedTrackLogs.size(); idx++){
                list .add( selectedTrackLogs.valueAt(idx).getName()  );
            }
            String msg = list.toString();
            builder.setMessage(msg.substring(1,msg.length()-1));

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    for(int idx = 0; idx < selectedTrackLogs.size(); idx++){
                        TrackLog aTrackLog = selectedTrackLogs.valueAt(idx);
                        application.metaTrackLogs.remove(aTrackLog.getNameKey());
                        application.availableTrackLogsObservable.availableTrackLogs.remove(aTrackLog);
                        if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == aTrackLog) {
                            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
                        }
                        Log.i(MGMapApplication.LABEL, NameUtil.context() +" confirm \""+item.getTitle()+"\" for track \"" + aTrackLog.getName()+"\"");
                        PersistenceManager.getInstance().deleteTrack(aTrackLog.getName());
                    }
                    TrackStatisticActivity.this.recreate();
                }
            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() +" abort \""+item.getTitle()+"\" for track \"" + trackLog.getName()+"\"");
                }
            });

            AlertDialog alert = builder.create();
            alert.show();

        }
        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_stl_track) )){
            Intent intent = new Intent(this, MGMapActivity.class);
            intent.putExtra("stl",trackLog.getNameKey());
            ArrayList<String> list = new ArrayList<>();
            for(int idx = 0; idx < selectedTrackLogs.size(); idx++){
                list .add( selectedTrackLogs.valueAt(idx).getNameKey()  );
            }
            list.remove(trackLog.getNameKey());
            if (list.size() > 0){
                intent.putExtra("atl",list.toString());
            }
            intent.setType("mgmap/showTrack");
            startActivity(intent);
        }
        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_mtl_track) )){
            Intent intent = new Intent(this, MGMapActivity.class);
            intent.putExtra("stl",trackLog.getNameKey());
            intent.setType("mgmap/markTrack");
            startActivity(intent);
//            application.getMS(MSMarker.class).createMarkerTrackLog(trackLog);
//            finish();
        }
        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_shr_track) ) || item.getTitle().equals( getResources().getString(R.string.ctx_stat_shr_track2) )){
            Intent sendIntent = null;
            String title = "Share ...";
            if (selectedTrackLogs.size() == 1){
                if (!PersistenceManager.getInstance().existsGpx(trackLog.getName())){ // if it is not yet persisted, then do it now before the share intent
                    GpxExporter.export(trackLog);
                }
                sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, PersistenceManager.getInstance().getGpxUri(trackLog.getName()));
                title = "Share "+trackLog.getName()+".gpx to ...";
            } else if (selectedTrackLogs.size() >= 2){
                ArrayList<Uri> uris = new ArrayList<>();
                for(int idx = 0; idx < selectedTrackLogs.size(); idx++){
                    TrackLog aTrackLog = selectedTrackLogs.valueAt(idx);
                    if (!PersistenceManager.getInstance().existsGpx(aTrackLog.getName())){ // if it is not yet persisted, then do it now before the share intent
                        GpxExporter.export(aTrackLog);
                    }
                    uris.add( PersistenceManager.getInstance().getGpxUri( aTrackLog.getName() ) );
                }
                sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            }
            sendIntent.setType("*/*");
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sendIntent, title));
        }
//        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_hpr_track) )){
//            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRefZoom(trackLog, -1, true));
//            application.availableTrackLogsObservable.changed();
//            Intent intent = new Intent(this, HeightProfileActivity.class);
//            startActivity(intent);
//        }
        if (item.getTitle().equals( getResources().getString(R.string.ctx_stat_sav_track) )){
            for(int idx = 0; idx < selectedTrackLogs.size(); idx++) {
                TrackLog aTrackLog = selectedTrackLogs.valueAt(idx);
                if (aTrackLog.isModified()){
                    GpxExporter.export(aTrackLog);
//                    application.metaTrackLogs.put(aTrackLog.getNameKey(), aTrackLog);
                    aTrackLog.setModified(false);
                }
            }
            finish();
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}

