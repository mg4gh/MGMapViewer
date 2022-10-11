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
package mg.mgmap.activity.statistic;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.StrictMode;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.HomeObserver;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.view.ExtendedTextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

public class TrackStatisticActivity extends AppCompatActivity {

    private Context context = null;
    private MGMapApplication application = null;
    private PersistenceManager persistenceManager = null;

    private LinearLayout parent = null;
    PrefCache prefCache = null;
    private ArrayList<TrackStatisticEntry> tseEntries = new ArrayList<>();

    private Pref<Boolean> prefFullscreen;
    private final Pref<Boolean> prefNoneSelected = new Pref<>(true);
    private final Pref<Boolean> prefAllSelected = new Pref<>(true);
    private final Pref<Boolean> prefEditAllowed = new Pref<>(true);
    private final Pref<Boolean> prefMarkerAllowed = new Pref<>(true);
    private final Pref<Boolean> prefDeleteAllowed = new Pref<>(true);
    private final Pref<Boolean> prefShareAllowed = new Pref<>(true);
    private final Pref<Boolean> prefNoneModified = new Pref<>(true);
    Pref<Boolean> prefFilterOn;
    final Pref<Boolean> prefFilterChanged = new Pref<>(true);

    TrackStatisticFilter filter;

    Observer reworkObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            timer.removeCallbacks(ttReworkState);
            timer.postDelayed(ttReworkState,100);
        }
    };

    Handler timer = new Handler();
    Runnable ttReworkState = () -> TrackStatisticActivity.this.runOnUiThread(this::reworkState);

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
        filter = new TrackStatisticFilter(prefCache);

        prefFilterOn.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                if (prefFilterOn.getValue()){
                    new TrackStatisticFilterDialog().show(context, TrackStatisticActivity.this);
                } else {
                    refreshVisibleEntries();
                }
            }
        });
        prefFilterChanged.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                if (prefFilterOn.getValue()){
                for (TrackStatisticEntry entry : tseEntries){
                    filter.checkFilter(entry);
                }
                }
                refreshVisibleEntries();
            }
        });

        prefFullscreen = prefCache.get(R.string.FSControl_qcFullscreenOn, true);
        prefFullscreen.addObserver((o, arg) -> FullscreenUtil.enforceState(TrackStatisticActivity.this, prefFullscreen.getValue()));
        Pref<Boolean> triggerHome = new Pref<>(true);
        triggerHome.addObserver( new HomeObserver(this) );

        parent = findViewById(R.id.trackStatisticEntries);

        ViewGroup qcs = findViewById(R.id.ts_qc);
//        ControlView.createQuickControlETV(qcs)
//                .setData(R.drawable.fullscreen)
//                .setPrAction(prefFullscreen,triggerHome);
        ControlView.createQuickControlETV(qcs)
                .setData(prefFilterOn,R.drawable.filter,R.drawable.filter2)
                .setPrAction(prefFilterOn);
        ExtendedTextView etvAll = ControlView.createQuickControlETV(qcs);
        etvAll.setData(prefAllSelected,R.drawable.select_all, R.drawable.select_all2)
                .setOnClickListener(new SelectOCL(parent, true));
        ExtendedTextView etvNone = ControlView.createQuickControlETV(qcs);
        etvNone.setData(prefNoneSelected,R.drawable.deselect_all,R.drawable.deselect_all2)
                .setOnClickListener(new SelectOCL(parent, false));
        qcs.removeView(etvNone);
        prefAllSelected.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
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
            }
        });
        ControlView.createQuickControlETV(qcs)
                .setData(prefEditAllowed,R.drawable.edit,R.drawable.edit2)
                .setOnClickListener(createEditOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefNoneSelected,R.drawable.show,R.drawable.show2)
                .setOnClickListener(createShowOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefMarkerAllowed,R.drawable.mtlr_2,R.drawable.mtlr)
                .setOnClickListener(createMarkerOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefShareAllowed,R.drawable.share2,R.drawable.share)
                .setOnClickListener(createShareOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefNoneModified,R.drawable.save,R.drawable.save2)
                .setOnClickListener(createSaveOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(prefDeleteAllowed,R.drawable.delete2,R.drawable.delete)
                .setOnClickListener(createDeleteOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.back)
                .setOnClickListener(createBackOCL());

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            }
        }
    }

    boolean working = false;

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MGMapApplication.LABEL, NameUtil.context());

        Set<String> nameKeys = new TreeSet<>();
        addTrackLog(nameKeys, parent, application.recordingTrackLogObservable.getTrackLog(), R.color.RED100_A100, R.color.RED100_A150);
//        addTrackLog(nameKeys, parent, application.markerTrackLogObservable.getTrackLog(), R.color.PINK_A100, R.color.PINK_A150);
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
                            runOnUiThread(() -> {
                                Log.v(MGMapApplication.LABEL, NameUtil.context()+" remainB working="+working+" size="+trackLogsRemain.size());
                                for (int i=0; (i<30)&&(!trackLogsRemain.isEmpty()) ; i++){
                                    TrackLog trackLog = trackLogsRemain.remove(0);
                                    addTrackLog(nameKeys, parent, trackLog, R.color.GRAY100_A100, R.color.GRAY100_A150);
                                }
                                Log.v(MGMapApplication.LABEL, NameUtil.context()+" remainC working="+working+" size="+trackLogsRemain.size());
                                working = false;
                            });

                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        prefFullscreen.onChange();
        reworkObserver.update(null,null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(MGMapApplication.LABEL, NameUtil.context());

        LinearLayout parent = findViewById(R.id.trackStatisticEntries);
        for (int idx=0; idx<parent.getChildCount(); idx++){
            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
                entry.onCleanup();
                entry.getPrefSelected().deleteObserver(reworkObserver);
                entry.getTrackLog().deleteObserver(reworkObserver);
            }
        }
        parent.removeAllViews();
        tseEntries.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ViewGroup qcs = findViewById(R.id.ts_qc);
        qcs.removeAllViews();
        prefCache.cleanup();
    }

    private void refreshVisibleEntries(){
        parent.removeAllViews();
        for (TrackStatisticEntry entry : getVisibleEntries()){
            parent.addView(entry);
        }
        reworkState();
    }

    private ArrayList<TrackStatisticEntry> getVisibleEntries(){
        return prefFilterOn.getValue()?getFilteredEntries():tseEntries;
    }

    private ArrayList<TrackStatisticEntry> getFilteredEntries(){
        ArrayList<TrackStatisticEntry> list = new ArrayList<>();
        for (TrackStatisticEntry entry : tseEntries){
            if (entry.isFilterMatched()){
                list.add(entry);
            }
        }
        return list;
    }

    private ArrayList<TrackStatisticEntry> getSelectedEntries(){
        ArrayList<TrackStatisticEntry> list = new ArrayList<>();
        for (TrackStatisticEntry entry : getVisibleEntries()){
            if (entry.isPrefSelected()){
                list.add(entry);
            }
        }
//        for (int idx=0; idx < parent.getChildCount(); idx++){
//            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
//                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
//                if (entry.isPrefSelected()){
//                    list.add(entry);
//                }
//            }
//        }
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



    // nameKeys contains the nameKey values of all tracks, which are already added
    public void addTrackLog(Set<String> nameKeys, ViewGroup parent, TrackLog trackLog, int colorId, int colorIdSelected){
        if (trackLog == null) return;
        if (nameKeys.contains(trackLog.getNameKey())) return; // this TrackLog is already added
        TrackLogStatistic statistic = trackLog.getTrackStatistic();
        if (statistic.getNumPoints() == 0) return; // don't show empty tracks, especially possible for marker Tracks
        nameKeys.add(trackLog.getNameKey());

        TrackStatisticEntry entry = new TrackStatisticEntry(context, trackLog, colorId, colorIdSelected);
        tseEntries.add(entry);
        filter.checkFilter(entry);
        if (!prefFilterOn.getValue() || entry.isFilterMatched()){
            parent.addView(entry);
        }
        entry.getPrefSelected().addObserver(reworkObserver);
        entry.getTrackLog().addObserver(reworkObserver);
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
        prefEditAllowed.setValue( entries.size() == 1 );
        boolean bMarker = (entries.size() == 1);
        if (bMarker){
            if (entries.get(0).getTrackLog() == application.routeTrackLogObservable.getTrackLog()) bMarker = false;
            if (entries.get(0).getTrackLog() == application.markerTrackLogObservable.getTrackLog()) bMarker = false;
        }
        prefMarkerAllowed.setValue(bMarker);
        boolean bDelete = (entries.size() > 0);
        if (bDelete){
            if (entries.get(0).getTrackLog() == application.recordingTrackLogObservable.getTrackLog()) bDelete = false;
            for (TrackStatisticEntry entry : entries){
                if (entry.getTrackLog() == application.markerTrackLogObservable.getTrackLog()) bDelete = false;
                if (entry.getTrackLog() == application.routeTrackLogObservable.getTrackLog()) bDelete = false;
            }
        }
        prefDeleteAllowed.setValue(bDelete);
        boolean bShare = (entries.size() > 0);
        if (bShare){
            for (TrackStatisticEntry entry : entries){
                TrackLog aTrackLog = entry.getTrackLog();
                if (!persistenceManager.existsGpx(aTrackLog.getName())) bShare = false;
            }
        }
        prefShareAllowed.setValue((bShare));
        prefNoneModified.setValue(getModifiedEntries().size() == 0);
    }

    private View.OnClickListener createEditOCL(){
        return v -> {
            if (prefEditAllowed.getValue()){
                final ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                if (entries.size() == 1){
                    TrackStatisticEntry tsEntry = entries.get(0);
                    TrackLog aTrackLog = tsEntry.getTrackLog();
                    final EditText etTrackLogName = new EditText(this);
                    etTrackLogName.setText(aTrackLog.getName());
                    etTrackLogName.setSelectAllOnFocus(true);
                    InputFilter filter = new InputFilter() {
                        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                            for (int i = start; i < end; i++) {
                                if ("/\\?%*:|\"<>.,;=\n".indexOf(source.charAt(i)) >= 0){
                                    etTrackLogName.setError("Not allowed characters: /\\?%*:|\"<>.,;=<LF>");
                                    return "";
                                }
                            }
                            return null;
                        }
                    };
                    etTrackLogName.setFilters(new InputFilter[] { filter });

                    new AlertDialog.Builder(this)
                            .setTitle("Rename Track")
                            .setMessage("Old name: "+aTrackLog.getName())
                            .setView(etTrackLogName)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String oldName = aTrackLog.getName();
                                    String oldNameKey = aTrackLog.getNameKey();
                                    String newName = etTrackLogName.getText().toString();
                                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" rename \""+oldName+"\" to \""+newName+"\"");

                                    if (persistenceManager.existsTrack(newName)){
                                        Toast.makeText(context, "Rename failed, name already exists: "+newName, Toast.LENGTH_LONG);
                                    } else{
                                        aTrackLog.setName(newName);
                                        persistenceManager.renameTrack(oldName, newName);
                                        application.metaTrackLogs.remove(oldNameKey);
                                        application.metaTrackLogs.put(aTrackLog.getNameKey(), aTrackLog);
                                        prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
                                    }
                                    tsEntry.invalidate();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
            }
        };
    }

    private View.OnClickListener createShowOCL(){
        return v -> {
            if (!prefNoneSelected.getValue()){
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
        };
    }

    private View.OnClickListener createMarkerOCL(){
        return v -> {
            if (prefMarkerAllowed.getValue()){
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();

                if (entries.size() == 1){
                    TrackLog trackLog = entries.get(0).getTrackLog();
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
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();
                if (entries.size() > 0){
                    Intent sendIntent;
                    String title = "Share ...";
                    if (entries.size() == 1){
                        TrackLog trackLog = entries.get(0).getTrackLog();
                        sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, persistenceManager.getGpxUri(trackLog.getName()));
                        title = "Share "+trackLog.getName()+".gpx to ...";
                    } else {
                        ArrayList<Uri> uris = new ArrayList<>();

                        for(TrackStatisticEntry entry : entries){
                            TrackLog aTrackLog = entry.getTrackLog();
                            uris.add( persistenceManager.getGpxUri( aTrackLog.getName() ) );
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
                ArrayList<TrackStatisticEntry> entries = getModifiedEntries();
                for (TrackStatisticEntry entry : entries){
                    TrackLog aTrackLog = entry.getTrackLog();
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" save "+aTrackLog.getName());
                    GpxExporter.export(persistenceManager, aTrackLog);
                    application.getMetaDataUtil().createMetaData(aTrackLog);
                }
                prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
            }
        };
    }

    private View.OnClickListener createDeleteOCL(){
        return v -> {
            if (prefDeleteAllowed.getValue()){
                ArrayList<TrackStatisticEntry> entries = getSelectedEntries();

                AlertDialog.Builder builder = new AlertDialog.Builder(TrackStatisticActivity.this);
                builder.setTitle(getResources().getString(R.string.ctx_stat_del_track));
                String msg = getNames(entries, false).toString();
                builder.setMessage(msg.substring(1,msg.length()-1));


                builder.setPositiveButton("YES", (dialog, which) -> {
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() +" confirm delete for list \""+msg+"\"");
                    for(TrackStatisticEntry entry : entries){
                        TrackLog aTrackLog = entry.getTrackLog();
                        application.metaTrackLogs.remove(aTrackLog.getNameKey());
                        application.availableTrackLogsObservable.availableTrackLogs.remove(aTrackLog);
                        if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == aTrackLog) {
                            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
                        }
                        persistenceManager.deleteTrack(aTrackLog.getName()); // no problem, if no persistent file exists
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

