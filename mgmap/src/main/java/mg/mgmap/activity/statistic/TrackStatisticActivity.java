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
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.text.InputFilter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TrackStatisticActivity extends AppCompatActivity {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private Context context = null;
    private MGMapApplication application = null;
    private PersistenceManager persistenceManager = null;

    private RecyclerView recyclerView = null;
    private TrackStatisticAdapter statisticAdapter = null;
    PrefCache prefCache = null;
    private final Set<String> allNameKeys = new TreeSet<>();
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

    final Handler timer = new Handler();
    final Runnable ttReworkState = () -> TrackStatisticActivity.this.runOnUiThread(this::reworkState);

    final Observer reworkObserver = (e) -> {
        timer.removeCallbacks(ttReworkState);
        timer.postDelayed(ttReworkState,30);
    };

    Pref<Boolean> metaLoading;
    final Observer metaDataObserver = (e) -> {
        if (!metaLoading.getValue()){ // loading is finished
            mgLog.d("rework allEntries allEntriesSize="+allEntries.size()+" metaTrackLogsSize="+application.metaTrackLogs.size());
            application.metaTrackLogs.values().forEach(this::addTrackLog);
            refreshVisibleEntries();
        }
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mgLog.d();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.track_statistic_activity);
        FullscreenUtil.init(findViewById(R.id.contentView));
        application = (MGMapApplication)getApplication();
        context = this;
        persistenceManager = application.getPersistenceManager();

        prefCache = new PrefCache(context);
        prefFilterOn = prefCache.get(R.string.Statistic_pref_FilterOn, false);
        metaLoading = prefCache.get(R.string.MGMapApplication_pref_MetaData_loading, true);

        prefFilterOn.addObserver((e) -> {
            if (prefFilterOn.getValue()){
                new TrackStatisticFilterDialog(TrackStatisticActivity.this).show();
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

        recyclerView = findViewById(R.id.trackStatisticEntries);
        statisticAdapter = new TrackStatisticAdapter(visibleEntries);
        recyclerView.setAdapter(statisticAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ViewGroup qcs = findViewById(R.id.ts_qc);
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefFilterOn,R.drawable.filter,R.drawable.filter2)
                .setPrAction(prefFilterOn)
                .setNameAndId(R.id.stat_mi_filter);
        ExtendedTextView etvAll = VUtil.createQuickControlETV(qcs,true);
        etvAll.setData(prefAllSelected,R.drawable.select_all, R.drawable.select_all2)
                .setNameAndId(R.id.stat_mi_all)
                .setOnClickListener(new SelectOCL(visibleEntries, true));
        ExtendedTextView etvNone = VUtil.createQuickControlETV(qcs,true);
        etvNone.setData(prefNoneSelected,R.drawable.deselect_all,R.drawable.deselect_all2)
                .setNameAndId(R.id.none)
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
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefEditAllowed,R.drawable.edit,R.drawable.edit2)
                .setNameAndId(R.id.stat_mi_edit)
                .setOnClickListener(createEditOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefNoneSelected,R.drawable.show,R.drawable.show2)
                .setNameAndId(R.id.stat_mi_show)
                .setOnClickListener(createShowOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefMarkerAllowed,R.drawable.mtlr_2,R.drawable.mtlr)
                .setNameAndId(R.id.stat_mi_marker)
                .setOnClickListener(createMarkerOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefShareAllowed,R.drawable.share2,R.drawable.share)
                .setNameAndId(R.id.stat_mi_share)
                .setOnClickListener(createShareOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefNoneModified,R.drawable.save,R.drawable.save2)
                .setNameAndId(R.id.stat_mi_save)
                .setOnClickListener(createSaveOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefDeleteAllowed,R.drawable.delete2,R.drawable.delete)
                .setNameAndId(R.id.stat_mi_delete)
                .setOnClickListener(createDeleteOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(R.drawable.back)
                .setNameAndId(R.id.stat_mi_back)
                .setOnClickListener(createBackOCL());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mgLog.d();

        addTrackLog(application.recordingTrackLogObservable.getTrackLog());
        addTrackLog(application.routeTrackLogObservable.getTrackLog());
        addTrackLog(application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        application.availableTrackLogsObservable.availableTrackLogs.forEach(this::addTrackLog);
        metaLoading.addObserver(metaDataObserver);
        application.metaTrackLogs.values().forEach(this::addTrackLog);
        refreshVisibleEntries();

        prefFullscreen.onChange();
        reworkObserver.propertyChange(null);
    }

    @Override
    protected void onPause() {
        mgLog.d();
        metaLoading.deleteObserver(metaDataObserver);
        visibleEntries.clear();
        allNameKeys.clear();
        allEntries.clear();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        application.metaTrackLogs.values().forEach(trackLog -> trackLog.getPrefSelected().deleteObserver(reworkObserver) );
        ViewGroup qcs = findViewById(R.id.ts_qc);
        qcs.removeAllViews();
        prefCache.cleanup();
        TrackStatisticView.cleanup();
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
    public void addTrackLog(TrackLog trackLog){
        if (trackLog == null) return;
        if (allNameKeys.contains(trackLog.getNameKey())) return; // this TrackLog is already added
        TrackLogStatistic statistic = trackLog.getTrackStatistic();
        if (statistic.getNumPoints() == 0) return; // don't show empty tracks, especially possible for marker Tracks
        allNameKeys.add(trackLog.getNameKey());

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
        mgLog.i(getNames(trackLogs, false));
        prefNoneSelected.setValue(trackLogs.isEmpty());
        prefAllSelected.setValue(visibleEntries.size() == trackLogs.size());
        prefEditAllowed.setValue( trackLogs.size() == 1 );
        boolean bMarker = (trackLogs.size() == 1);
        if (bMarker){
            if (trackLogs.get(0) == application.routeTrackLogObservable.getTrackLog()) bMarker = false;
            if (trackLogs.get(0) == application.markerTrackLogObservable.getTrackLog()) bMarker = false;
        }
        prefMarkerAllowed.setValue(bMarker);
        boolean bDelete = (!trackLogs.isEmpty());
        if (bDelete){
            if (trackLogs.get(0) == application.recordingTrackLogObservable.getTrackLog()) bDelete = false;
            for (TrackLog trackLog : trackLogs){
                if (trackLog == application.markerTrackLogObservable.getTrackLog()) bDelete = false;
                if (trackLog == application.routeTrackLogObservable.getTrackLog()) bDelete = false;
            }
        }
        prefDeleteAllowed.setValue(bDelete);
        boolean bShare = (!trackLogs.isEmpty());
        if (bShare){
            for (TrackLog trackLog : trackLogs){
                if (!persistenceManager.existsGpx(trackLog.getName())) bShare = false;
            }
        }
        prefShareAllowed.setValue((bShare));
        prefNoneModified.setValue(getModifiedEntries().isEmpty());
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
                            if ("/\\?%*:|\"<>.,;=\n".indexOf(source.charAt(i)) >= 0){
//                                etTrackLogName.setError("Not allowed characters: /\\?%*:|\"<>.,;=<LF>");
                                return "";
                            }
                        }
                        return null;
                    };
                    etTrackLogName.setFilters(new InputFilter[] { filter });

                    DialogView dialogView = this.findViewById(R.id.dialog_parent);
                    dialogView.lock(() -> dialogView
                            .setTitle("Rename Track")
                            .setMessage("Old name: "+trackLog.getName())
                            .setContentView(etTrackLogName)
                            .setPositive("OK", evt -> {
                                String oldName = trackLog.getName();
                                String oldNameKey = trackLog.getNameKey();
                                String newName = etTrackLogName.getText().toString();
                                mgLog.i("rename \""+oldName+"\" to \""+newName+"\"");
                                if (!newName.equals(oldName)){
                                    if (persistenceManager.existsTrack(newName)){
                                        Toast.makeText(context, "Rename failed, name already exists: "+newName, Toast.LENGTH_LONG).show();
                                    } else{
                                        if (newName.contains("/")){
                                            String path = newName.replaceFirst("/[^/]*$","");
                                            mgLog.i("check path="+path);
                                            persistenceManager.createTrackPath(path);
                                        }
                                        trackLog.setName(newName);
                                        persistenceManager.renameTrack(oldName, newName);
                                        application.metaTrackLogs.remove(oldNameKey);
                                        application.metaTrackLogs.put(trackLog.getNameKey(), trackLog);
                                        prefCache.get(R.string.preferences_sftp_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
                                    }
                                    statisticAdapter.notifyItemChanged(visibleEntries.indexOf(trackLog));
                                }
                            })
                            .setNegative("Cancel", null)
                            .show());
                }
            }
        };
    }

    private View.OnClickListener createShowOCL(){
        return v -> {
            if (!prefNoneSelected.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();
                if (!trackLogs.isEmpty()){
                    TrackLog sel = trackLogs.remove(0);
                    Intent intent = new Intent(TrackStatisticActivity.this, MGMapActivity.class);
                    intent.putExtra("stl",sel.getNameKey());
                    List<String> list = getNames(trackLogs,true);
                    if (!list.isEmpty()){
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
                if (!trackLogs.isEmpty()){
                    Intent sendIntent;
                    String title = "Share ...";
                    if (trackLogs.size() == 1){
                        TrackLog trackLog = trackLogs.get(0);
                        sendIntent = new Intent(Intent.ACTION_SEND);
                        Uri uri = persistenceManager.getGpxUri(trackLog.getName());
                        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        sendIntent.setClipData(ClipData.newRawUri("", uri));
                        title = "Share "+trackLog.getName()+".gpx to ...";
                    } else {
                        ArrayList<Uri> uris = new ArrayList<>();
                        ClipData clipData = ClipData.newRawUri("", null);
                        for(TrackLog trackLog : trackLogs){
                            Uri uri = persistenceManager.getGpxUri(trackLog.getName());
                            uris.add( uri );
                            clipData.addItem(new ClipData.Item(uri));
                        }
                        sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                        sendIntent.setClipData(clipData);
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
                    mgLog.i("save "+trackLog.getName());
                    GpxExporter.export(persistenceManager, trackLog);
                    application.getMetaDataUtil().createMetaData(trackLog);
                    application.getMetaDataUtil().writeMetaData(application.getPersistenceManager().openMetaOutput(trackLog.getName()), trackLog);
                }
                reworkObserver.propertyChange(null);
                prefCache.get(R.string.preferences_sftp_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
            }
        };
    }

    private View.OnClickListener createDeleteOCL(){
        return v -> {
            if (prefDeleteAllowed.getValue()){
                ArrayList<TrackLog> trackLogs = getSelectedEntries();
                List<String> names = getNames(trackLogs, false);
                DialogView dialogView = this.findViewById(R.id.dialog_parent);
                dialogView.lock(() -> dialogView
                        .setTitle(getResources().getString(R.string.ctx_stat_del_track))
                        .setContentView( VUtil.createFileListView(context, names) )
                        .setPositive("OK", evt -> {
                            mgLog.i("confirm delete for list \""+names+"\"");
                            for(TrackLog trackLog : trackLogs){
                                application.metaTrackLogs.remove(trackLog.getNameKey());
                                application.availableTrackLogsObservable.availableTrackLogs.remove(trackLog);
                                if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == trackLog) {
                                    application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
                                }
                                persistenceManager.deleteTrack(trackLog.getName()); // no problem, if no persistent file exists
                            }
                            TrackStatisticActivity.this.recreate();
                        })
                        .setNegative("Cancel", null)
                        .show());
            }
        };
    }

    private View.OnClickListener createBackOCL(){
        return v -> TrackStatisticActivity.this.onBackPressed();
    }


}

