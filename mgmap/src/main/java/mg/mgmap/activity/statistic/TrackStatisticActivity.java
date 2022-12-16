/*
 * Copyright 2017 - 2022 mg4gh
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
package mg.mgmap.activity.statistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.text.InputFilter;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.view.ExtendedTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TrackStatisticActivity extends AppCompatActivity {

    private Context context = null;
    private MGMapApplication application = null;
    private PersistenceManager persistenceManager = null;

    private RecyclerView recyclerView = null;
    private TrackStatisticAdapter statisticAdapter = null;
    PrefCache prefCache = null;
    private final ArrayList<TrackLog> allEntries = new ArrayList<>();
    private final ArrayList<TrackLog> visibleEntries = new ArrayList<>();

    private Pref<Boolean> prefFullscreen;
    private final Pref<Boolean> prefNoneSelected = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefAllSelected = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefEditAllowed = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefMarkerAllowed = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefDeleteAllowed = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefShareAllowed = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefNoneModified = new Pref<>(Boolean.TRUE);
    Pref<Boolean> prefFilterOn;
    final Pref<Boolean> prefFilterChanged = new Pref<>(Boolean.TRUE);

    public MGMapApplication getMGMapApplication() {
        return application;
    }

    Handler timer = new Handler();
    Runnable ttReworkState = () -> TrackStatisticActivity.this.runOnUiThread(this::reworkState);

    Observer reworkObserver = (e) -> {
        timer.removeCallbacks(ttReworkState);
        timer.postDelayed(ttReworkState,30);
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.track_statistic_activity);
        application = (MGMapApplication)getApplication();
        context = this;
        persistenceManager = application.getPersistenceManager();

        prefCache = new PrefCache(context);
        prefFilterOn = prefCache.get(R.string.Statistic_pref_FilterOn, false);

        prefFilterOn.addObserver((e) -> {
            if (prefFilterOn.getValue()){
                new TrackStatisticFilterDialog().show(context, TrackStatisticActivity.this);
            } else {
                refreshVisibleEntries();
            }
        });
        prefFilterChanged.addObserver((e) -> {
            TrackStatisticFilter filter = application.getTrackStatisticFilter();
            if (prefFilterOn.getValue()){
                for (TrackLog entry : allEntries){
                    filter.checkFilter(entry);
                }
            }
            refreshVisibleEntries();
        });

        prefFullscreen = prefCache.get(R.string.FSControl_qcFullscreenOn, true);
        prefFullscreen.addObserver((e) -> FullscreenUtil.enforceState(TrackStatisticActivity.this, prefFullscreen.getValue()));

        recyclerView = (RecyclerView) findViewById(R.id.trackStatisticEntries);
        statisticAdapter = new TrackStatisticAdapter(visibleEntries);
        recyclerView.setAdapter(statisticAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ViewGroup qcs = findViewById(R.id.ts_qc);
        ControlView.createQuickControlETV(qcs)
                .setData(prefFilterOn,R.drawable.filter,R.drawable.filter2)
                .setPrAction(prefFilterOn)
                .setName("tsaQcFilter");
        ExtendedTextView etvAll = ControlView.createQuickControlETV(qcs);
        etvAll.setData(prefAllSelected,R.drawable.select_all, R.drawable.select_all2)
                .setName("tsaQcSelectAll")
                .setOnClickListener(new SelectOCL(visibleEntries, true));
        ExtendedTextView etvNone = ControlView.createQuickControlETV(qcs);
        etvNone.setData(prefNoneSelected,R.drawable.deselect_all,R.drawable.deselect_all2)
                .setName("tsaQcSelectNone>")
                .setOnClickListener(new SelectOCL(visibleEntries, false));
        qcs.removeView(etvNone);
        prefAllSelected.addObserver((e) -> {
            if (prefAllSelected.getValue()){
                if ((etvAll.getParent() != null) && (etvNone.getParent() == null)) {
                    qcs.addView(etvNone, qcs.indexOfChild(etvAll));
                    qcs.removeView(etvAll);
                }
            } else {
                if ((etvAll.getParent() == null) && (etvNone.getParent() != null)){
                    qcs.addView(etvAll, qcs.indexOfChild(etvNone));
                    qcs.removeView(etvNone);
                }
            }
        });
        ControlView.createQuickControlETV(qcs)
                .setData(prefEditAllowed,R.drawable.edit,R.drawable.edit2)
                .setName("tsaQcEdit")
                .setOnClickListener(createEditOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefNoneSelected,R.drawable.show,R.drawable.show2)
                .setName("tsaQcShow")
                .setOnClickListener(createShowOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefMarkerAllowed,R.drawable.mtlr_2,R.drawable.mtlr)
                .setName("tsaQcShowMtlr")
                .setOnClickListener(createMarkerOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefShareAllowed,R.drawable.share2,R.drawable.share)
                .setName("tsaQcShare")
                .setOnClickListener(createShareOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefNoneModified,R.drawable.save,R.drawable.save2)
                .setName("tsaQcSave")
                .setOnClickListener(createSaveOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefDeleteAllowed,R.drawable.delete2,R.drawable.delete)
                .setName("tsaQcShowDelete")
                .setOnClickListener(createDeleteOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.back)
                .setName("tsaQcBack")
                .setOnClickListener(createBackOCL());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MGMapApplication.LABEL, NameUtil.context());

        Set<String> nameKeys = new TreeSet<>();
        addTrackLog(nameKeys, application.recordingTrackLogObservable.getTrackLog());
        addTrackLog(nameKeys, application.routeTrackLogObservable.getTrackLog());
        addTrackLog(nameKeys, application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        for (TrackLog trackLog : application.availableTrackLogsObservable.availableTrackLogs){
            addTrackLog(nameKeys, trackLog);
        }
        for (TrackLog trackLog : application.metaTrackLogs.values()) {
            addTrackLog(nameKeys, trackLog);
        }
        refreshVisibleEntries();

        prefFullscreen.onChange();
        reworkObserver.propertyChange(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        visibleEntries.clear();
        allEntries.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ViewGroup qcs = findViewById(R.id.ts_qc);
        qcs.removeAllViews();
        prefCache.cleanup();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void refreshVisibleEntries(){
        visibleEntries.clear();
        for (TrackLog trackLog : allEntries){
            if (!prefFilterOn.getValue()
                    || trackLog.isFilterMatched()
                    || (trackLog==application.recordingTrackLogObservable.getTrackLog())
                    || (trackLog==application.routeTrackLogObservable.getTrackLog())
                    || (application.availableTrackLogsObservable.availableTrackLogs.contains(trackLog))   ){
                visibleEntries.add(trackLog);
            }
        }
        statisticAdapter.notifyDataSetChanged();
        reworkState();
    }



    private ArrayList<TrackLog> getSelectedEntries(){
        ArrayList<TrackLog> list = new ArrayList<>();
        for (TrackLog trackLog : visibleEntries){
            if (trackLog.isSelected()){
                list.add(trackLog);
            }
        }
        return list;
    }
    private ArrayList<TrackLog> getModifiedEntries(){
        ArrayList<TrackLog> list = new ArrayList<>();
        for (TrackLog trackLog : getSelectedEntries()){
            if (trackLog.isModified()){
                list.add(trackLog);
            }
        }
        return list;
    }



    // nameKeys contains the nameKey values of all tracks, which are already added
    public void addTrackLog(Set<String> nameKeys, TrackLog trackLog){
        if (trackLog == null) return;
        if (nameKeys.contains(trackLog.getNameKey())) return; // this TrackLog is already added
        TrackLogStatistic statistic = trackLog.getTrackStatistic();
        if (statistic.getNumPoints() == 0) return; // don't show empty tracks, especially possible for marker Tracks
        nameKeys.add(trackLog.getNameKey());

        allEntries.add(trackLog);
        trackLog.getPrefSelected().addObserver(reworkObserver);
    }

    private List<String> getNames(List<TrackLog> entries, boolean useNameKeys){
        ArrayList<String> list = new ArrayList<>();
        for (TrackLog trackLog : entries){
            list.add((useNameKeys)?trackLog.getNameKey():trackLog.getName()); // make a list of strings out of it
        }
        return list;
    }

    private void reworkState(){
        ArrayList<TrackLog> trackLogs = getSelectedEntries();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+ getNames(trackLogs, false));
        prefNoneSelected.setValue(trackLogs.size() == 0);
        prefAllSelected.setValue(visibleEntries.size() == trackLogs.size());
        prefEditAllowed.setValue( trackLogs.size() == 1 );
        boolean bMarker = (trackLogs.size() == 1);
        if (bMarker){
            if (trackLogs.get(0) == application.routeTrackLogObservable.getTrackLog()) bMarker = false;
            if (trackLogs.get(0) == application.markerTrackLogObservable.getTrackLog()) bMarker = false;
        }
        prefMarkerAllowed.setValue(bMarker);
        boolean bDelete = (trackLogs.size() > 0);
        if (bDelete){
            if (trackLogs.get(0) == application.recordingTrackLogObservable.getTrackLog()) bDelete = false;
            for (TrackLog trackLog : trackLogs){
                if (trackLog == application.markerTrackLogObservable.getTrackLog()) bDelete = false;
                if (trackLog == application.routeTrackLogObservable.getTrackLog()) bDelete = false;
            }
        }
        prefDeleteAllowed.setValue(bDelete);
        boolean bShare = (trackLogs.size() > 0);
        if (bShare){
            for (TrackLog trackLog : trackLogs){
                if (!persistenceManager.existsGpx(trackLog.getName())) bShare = false;
            }
        }
        prefShareAllowed.setValue((bShare));
        prefNoneModified.setValue(getModifiedEntries().size() == 0);
    }

    private View.OnClickListener createEditOCL(){
        return v -> {
            if (prefEditAllowed.getValue()){
                final ArrayList<TrackLog> trackLogs = getSelectedEntries();
                if (trackLogs.size() == 1){
                    TrackLog trackLog = trackLogs.get(0);
                    final EditText etTrackLogName = new EditText(this);
                    etTrackLogName.setText(trackLog.getName());
                    etTrackLogName.setSelectAllOnFocus(true);
                    InputFilter filter = (source, start, end, dest, dstart, dend) -> {
                        for (int i = start; i < end; i++) {
                            if ("\\?%*:|\"<>.,;=\n".indexOf(source.charAt(i)) >= 0){
//                                etTrackLogName.setError("Not allowed characters: /\\?%*:|\"<>.,;=<LF>");
                                return "";
                            }
                        }
                        return null;
                    };
                    etTrackLogName.setFilters(new InputFilter[] { filter });

                    new AlertDialog.Builder(this)
                            .setTitle("Rename Track")
                            .setMessage("Old name: "+trackLog.getName())
                            .setView(etTrackLogName)
                            .setCancelable(false)
                            .setPositiveButton("OK", (dialog, whichButton) -> {
                                String oldName = trackLog.getName();
                                String oldNameKey = trackLog.getNameKey();
                                String newName = etTrackLogName.getText().toString();
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" rename \""+oldName+"\" to \""+newName+"\"");
                                if (!newName.equals(oldName)){
                                    if (persistenceManager.existsTrack(newName)){
                                        Toast.makeText(context, "Rename failed, name already exists: "+newName, Toast.LENGTH_LONG).show();
                                    } else{
                                        if (newName.contains("/")){
                                            String path = newName.replaceFirst("/[^/]*$","");
                                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" check path="+path);
                                            persistenceManager.createTrackPath(path);
                                        }
                                        trackLog.setName(newName);
                                        persistenceManager.renameTrack(oldName, newName);
                                        application.metaTrackLogs.remove(oldNameKey);
                                        application.metaTrackLogs.put(trackLog.getNameKey(), trackLog);
                                        prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
                                    }
                                    statisticAdapter.notifyItemChanged(visibleEntries.indexOf(trackLog));
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, whichButton) -> {})
                            .show();
                }
            }
        };
    }

    private View.OnClickListener createShowOCL(){
        return v -> {
            if (!prefNoneSelected.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();
                if (trackLogs.size() > 0){
                    TrackLog sel = trackLogs.remove(0);
                    Intent intent = new Intent(TrackStatisticActivity.this, MGMapActivity.class);
                    intent.putExtra("stl",sel.getNameKey());
                    List<String> list = getNames(trackLogs,true);
                    if (list.size() > 0){
                        intent.putExtra("atl",list.toString());
                    }
                    intent.setType("mgmap/showTrack");
                    startActivity(intent);
                }
            }
        };
    }

    private View.OnClickListener createMarkerOCL(){
        return v -> {
            if (prefMarkerAllowed.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();

                if (trackLogs.size() == 1){
                    TrackLog trackLog = trackLogs.get(0);
                    Intent intent = new Intent(TrackStatisticActivity.this, MGMapActivity.class);
                    intent.putExtra("stl",trackLog.getNameKey());
                    intent.setType("mgmap/markTrack");
                    startActivity(intent);
                }
            }
        };
    }

    private View.OnClickListener createShareOCL(){
        return v -> {
            if (prefShareAllowed.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();
                if (trackLogs.size() > 0){
                    Intent sendIntent;
                    String title = "Share ...";
                    if (trackLogs.size() == 1){
                        TrackLog trackLog = trackLogs.get(0);
                        sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, persistenceManager.getGpxUri(trackLog.getName()));
                        title = "Share "+trackLog.getName()+".gpx to ...";
                    } else {
                        ArrayList<Uri> uris = new ArrayList<>();

                        for(TrackLog trackLog : trackLogs){
                            uris.add( persistenceManager.getGpxUri( trackLog.getName() ) );
                        }
                        sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                    }
                    sendIntent.setType("*/*");
                    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(sendIntent, title));
                }
            }
        };
    }

    private View.OnClickListener createSaveOCL(){
        return v -> {
            if (!prefNoneModified.getValue()){
                ArrayList<TrackLog> trackLogs = getModifiedEntries();
                for (TrackLog trackLog : trackLogs){
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" save "+trackLog.getName());
                    GpxExporter.export(persistenceManager, trackLog);
                    application.getMetaDataUtil().createMetaData(trackLog);
                }
                prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
            }
        };
    }

    private View.OnClickListener createDeleteOCL(){
        return v -> {
            if (prefDeleteAllowed.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();

                AlertDialog.Builder builder = new AlertDialog.Builder(TrackStatisticActivity.this);
                builder.setTitle(getResources().getString(R.string.ctx_stat_del_track));
                String msg = getNames(trackLogs, false).toString();
                builder.setMessage(msg.substring(1,msg.length()-1));


                builder.setPositiveButton("YES", (dialog, which) -> {
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() +" confirm delete for list \""+msg+"\"");
                    for(TrackLog trackLog : trackLogs){
                        application.metaTrackLogs.remove(trackLog.getNameKey());
                        application.availableTrackLogsObservable.availableTrackLogs.remove(trackLog);
                        if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == trackLog) {
                            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
                        }
                        persistenceManager.deleteTrack(trackLog.getName()); // no problem, if no persistent file exists
                    }
                    TrackStatisticActivity.this.recreate();
                });

                builder.setNegativeButton("NO", (dialog, which) -> {
                    // Do nothing
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() +" abort delete for list \""+msg+"\"");
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        };
    }

    private View.OnClickListener createBackOCL(){
        return v -> TrackStatisticActivity.this.onBackPressed();
    }


}

