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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import mg.mapviewer.ControlView;
import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.fullscreen.MSFullscreen;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.util.GpxExporter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;
import mg.mapviewer.view.TrackStatisticEntry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class TrackStatisticActivity extends AppCompatActivity {

    private Context context = null;
    private MGMapApplication application = null;

    LinearLayout parent = null;
    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.MSFullscreen_qc_On, true);
    private final MGPref<Boolean> prefNoneSelected = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);
    private final MGPref<Boolean> prefAllSelected = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);
    private final MGPref<Boolean> prefMarkerAllowed = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);
    private final MGPref<Boolean> prefDeleteAllowed = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);
    private final MGPref<Boolean> prefShareAllowed = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);
    private final MGPref<Boolean> prefNoneModified = new MGPref<Boolean>(UUID.randomUUID().toString(), true, false);

    Observer reworkObserver = null;

    Handler timer = new Handler();
    Runnable ttReworkState = new Runnable() {
        @Override
        public void run() {
            TrackStatisticActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reworkState();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        setContentView(R.layout.track_statistic_activity);
        application = (MGMapApplication)getApplication();
//        context = application.getApplicationContext();
        context = this;

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
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        parent = findViewById(R.id.trackStatisticEntries);

        reworkObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                timer.removeCallbacks(ttReworkState);
                timer.postDelayed(ttReworkState,100);
            }
        };


        Set<String> nameKeys = new TreeSet<>();
        addTrackLog(nameKeys, parent, application.recordingTrackLogObservable.getTrackLog(), R.color.RED100_A100, R.color.RED100_A150);
        addTrackLog(nameKeys, parent, application.markerTrackLogObservable.getTrackLog(), R.color.PINK_A100, R.color.PINK_A150);
        addTrackLog(nameKeys, parent, application.routeTrackLogObservable.getTrackLog(), R.color.PURPLE_A100, R.color.PURPLE_A150);
        addTrackLog(nameKeys, parent, application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog(), R.color.BLUE100_A100, R.color.BLUE150_A150);
        for (TrackLog trackLog : application.availableTrackLogsObservable.availableTrackLogs){
            addTrackLog(nameKeys, parent, trackLog, R.color.GREEN100_A100, R.color.GREEN150_A150);
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
                                        addTrackLog(nameKeys, parent, trackLog, R.color.GRAY100_A100, R.color.GRAY100_A150);
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

        initQuickControls();
        reworkObserver.update(null,null);
        reworkObserver.update(null,null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(MGMapApplication.LABEL, NameUtil.context());

        LinearLayout parent = findViewById(R.id.trackStatisticEntries);
        for (int idx=0; idx<parent.getChildCount(); idx++){
            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
                entry.onCleanup();
                entry.getPrefSelected().deleteObserver(reworkObserver);
                entry.getTrackLog().getPrefModified().deleteObserver(reworkObserver);
            }
        }
        parent.removeAllViews();
        ViewGroup qcs = findViewById(R.id.ts_qc);
        qcs.removeAllViews();
    }

    private void initQuickControls(){
        ViewGroup qcs = findViewById(R.id.ts_qc);

        ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefFullscreen}, new int[]{R.drawable.fullscreen, R.drawable.fullscreen});
        prefFullscreen.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (prefFullscreen.getValue()){
                    MSFullscreen.setFullscreen(TrackStatisticActivity.this);
                } else {
                    MSFullscreen.hideFullscreen(TrackStatisticActivity.this);
                }
            }
        });
        prefFullscreen.onChange();


        PrefTextView selectAll = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefAllSelected}, new int[]{R.drawable.select_all, R.drawable.select_all2});
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedAll(true);
            }
        });
        PrefTextView deselectAll = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefNoneSelected}, new int[]{R.drawable.deselect_all,R.drawable.deselect_all2});
        deselectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedAll(false);
            }
        });

        PrefTextView show = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefNoneSelected}, new int[]{R.drawable.show,R.drawable.show2});
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                if (entries.size() > 0){
                    TrackStatisticEntry sel = entries.remove(0);
                    Intent intent = new Intent(TrackStatisticActivity.this, MGMapActivity.class);
                    intent.putExtra("stl",sel.getTrackLog().getNameKey());
                    List<String> list = getNames(entries,true);
                    if (list.size() > 0){
                        intent.putExtra("atl",list.toString());
                    }
                    intent.setType("mgmap/showTrack");
                    startActivity(intent);
                }
            }
        });

        PrefTextView mtlr = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefMarkerAllowed}, new int[]{R.drawable.mtlr,R.drawable.mtlr_2});
        mtlr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                if (entries.size() == 1){
                    TrackLog trackLog = entries.get(0).getTrackLog();
                    Intent intent = new Intent(TrackStatisticActivity.this, MGMapActivity.class);
                    intent.putExtra("stl",trackLog.getNameKey());
                    intent.setType("mgmap/markTrack");
                    startActivity(intent);
                }
            }
        });


        PrefTextView share = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefShareAllowed}, new int[]{R.drawable.share,R.drawable.share2});
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                if (entries.size() > 0){
                    Intent sendIntent = null;
                    String title = "Share ...";
                    if (entries.size() == 1){
                        TrackLog trackLog = entries.get(0).getTrackLog();
                        if (!PersistenceManager.getInstance().existsGpx(trackLog.getName())){ // if it is not yet persisted, then do it now before the share intent
                            GpxExporter.export(trackLog);
                        }
                        sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, PersistenceManager.getInstance().getGpxUri(trackLog.getName()));
                        title = "Share "+trackLog.getName()+".gpx to ...";
                    } else if (entries.size() >= 2){
                        ArrayList<Uri> uris = new ArrayList<>();

                        for(TrackStatisticEntry entry : entries){
                            TrackLog aTrackLog = entry.getTrackLog();
                            if (!PersistenceManager.getInstance().existsGpx(aTrackLog.getName())){ // if it is not yet persisted, then do it now before the share intent
                                GpxExporter.export(aTrackLog);
//                                aTrackLog.setModified(false);
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
            }
        });



        PrefTextView save = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefNoneModified}, new int[]{R.drawable.save,R.drawable.save2});
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                for (TrackStatisticEntry entry : entries){
                    if (entry.isModified()){
                        TrackLog aTrackLog = entry.getTrackLog();
                        GpxExporter.export(aTrackLog);
                    }
                }
            }
        });

        PrefTextView delete = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{prefDeleteAllowed}, new int[]{R.drawable.delete,R.drawable.delete2});
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();

                AlertDialog.Builder builder = new AlertDialog.Builder(TrackStatisticActivity.this);
                builder.setTitle(getResources().getString(R.string.ctx_stat_del_track));
                String msg = getNames(entries, false).toString();
                builder.setMessage(msg.substring(1,msg.length()-1));


                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Log.i(MGMapApplication.LABEL, NameUtil.context() +" confirm delete for list \""+msg+"\"");
                        for(TrackStatisticEntry entry : entries){
                            TrackLog aTrackLog = entry.getTrackLog();
                            application.metaTrackLogs.remove(aTrackLog.getNameKey());
                            application.availableTrackLogsObservable.availableTrackLogs.remove(aTrackLog);
                            if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == aTrackLog) {
                                application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
                            }
                            PersistenceManager.getInstance().deleteTrack(aTrackLog.getName()); // no problem, if no persistent file exists
                        }
                        TrackStatisticActivity.this.recreate();
                    }
                });

                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                        dialog.dismiss();
                        Log.i(MGMapApplication.LABEL, NameUtil.context() +" abort delete for list \""+msg+"\"");
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });


        PrefTextView ptvBack = ControlView.createQuickControlPTV(qcs,20).setPrefData(new MGPref[]{}, new int[]{R.drawable.back});
        ptvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackStatisticActivity.this.finish();
            }
        });

    }

    private ArrayList<TrackStatisticEntry> getSelectedEntries(){
        ArrayList<TrackStatisticEntry> list = new ArrayList<>();
        for (int idx=0; idx < parent.getChildCount(); idx++){
            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
                if (entry.isPrefSelected()){
                    list.add(entry);
                }
            }
        }
        return list;
    }
    private ArrayList<TrackStatisticEntry> getModifiedEntries(){
        ArrayList<TrackStatisticEntry> list = new ArrayList<>();
        for (TrackStatisticEntry entry : getSelectedEntries()){
            if (entry.isModified()){
                list.add(entry);
            }
        }
        return list;
    }

    private void setSelectedAll(boolean selected){
        for (int idx=0; idx < parent.getChildCount(); idx++){
            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
                entry.setPrefSelected(selected);
            }
        }
    }

    private int convertDp(float dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // nameKeys contains the nameKey values of all tracks, which are already added
    public void addTrackLog(Set<String> nameKeys, ViewGroup parent, TrackLog trackLog, int colorId, int colorIdSelected){
        if (trackLog == null) return;
        if (nameKeys.contains(trackLog.getNameKey())) return; // this TrackLog is already added
        TrackLogStatistic statistic = trackLog.getTrackStatistic();
        if (statistic.getNumPoints() == 0) return; // don't show empty tracks, especially possible for marker Tracks
        nameKeys.add(trackLog.getNameKey());

        TrackStatisticEntry entry = new TrackStatisticEntry(context, trackLog, parent, colorId, colorIdSelected);
        entry.getPrefSelected().addObserver(reworkObserver);
        entry.getTrackLog().getPrefModified().addObserver(reworkObserver);
    }

    private List<String> getNames(List<TrackStatisticEntry> entries, boolean useNameKeys){
        ArrayList<String> list = new ArrayList<>();
        for (TrackStatisticEntry entry : entries){
            TrackLog trackLog = entry.getTrackLog();
            list.add((useNameKeys)?trackLog.getNameKey():trackLog.getName()); // make a list of strings out of it
        }
        return list;
    }

    private void reworkState(){
        ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+ getNames(entries, false));
        prefNoneSelected.setValue(entries.size() == 0);
        prefAllSelected.setValue(parent.getChildCount() == entries.size());
        boolean bMarker = (entries.size() == 1);
        if (bMarker){
            if (entries.get(0).getTrackLog() == application.routeTrackLogObservable.getTrackLog()) bMarker = false;
            if (entries.get(0).getTrackLog() == application.markerTrackLogObservable.getTrackLog()) bMarker = false;
        }
        prefMarkerAllowed.setValue(!bMarker);
        boolean bDelete = (entries.size() > 0);
        if (bDelete){
            if (entries.get(0).getTrackLog() == application.recordingTrackLogObservable.getTrackLog()) bDelete = false;
            for (TrackStatisticEntry entry : entries){
                if (entry.getTrackLog() == application.markerTrackLogObservable.getTrackLog()) bDelete = false;
                if (entry.getTrackLog() == application.routeTrackLogObservable.getTrackLog()) bDelete = false;
            }
        }
        prefDeleteAllowed.setValue(!bDelete);
        boolean bShare = (entries.size() > 0);
        if (bShare){
            for (TrackStatisticEntry entry : entries){
                TrackLog aTrackLog = entry.getTrackLog();
                if (!PersistenceManager.getInstance().existsGpx(aTrackLog.getName())) bShare = false;
            }
        }
        prefShareAllowed.setValue((!bShare));
        prefNoneModified.setValue(getModifiedEntries().size() == 0);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
