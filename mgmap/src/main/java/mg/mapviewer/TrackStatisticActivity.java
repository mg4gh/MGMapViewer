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
package mg.mapviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.StrictMode;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;

import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.TrackLogStatistic;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class TrackStatisticActivity extends Activity {

    private SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    private SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);

    private Context context = null;
    private MGMapApplication application = null;
    private Resources resources = null;

    private SparseArray<TrackLogRef> selectionRefs = new SparseArray<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_main);
        application = (MGMapApplication)getApplication();
        context = application.getApplicationContext();
        resources = getResources();

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


    @Override
    protected void onStart() {
        super.onStart();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout parent = findViewById(R.id.trackView);

        addTrackLog(parent, application.recordingTrackLogObservable.getTrackLog(), R.color.RED100_A100);
        addTrackLog(parent, application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog(), R.color.BLUE100_A100);
        for (TrackLog trackLog : application.availableTrackLogsObservable.availableTrackLogs){
            if (trackLog != application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog()){
                addTrackLog(parent, trackLog, R.color.GREEN100_A100);
            }
        }
        for (TrackLog trackLog : application.metaTrackLogs){
            if (!application.availableTrackLogsObservable.availableTrackLogs.contains(trackLog)){
                addTrackLog(parent, trackLog, R.color.GRAY100_A100);
            }
        }
    }

    static Random random = new Random(System.currentTimeMillis());
    public void addTrackLog(ViewGroup parent, TrackLog trackLog, int colorId){
        if (trackLog == null) return;
        TrackLogStatistic statistic = trackLog.getTrackStatistic();


        ViewStub vs = new ViewStub(context);
        vs.setLayoutResource(R.layout.statistic_entry);
        parent.addView(vs);
        TableLayout tableLayout = (TableLayout)vs.inflate();
        tableLayout.setId(random.nextInt());
        selectionRefs.put(tableLayout.getId(), new TrackLogRef(trackLog, -1));
        registerForContextMenu(tableLayout);

        TableRow tableRow0 = (TableRow)tableLayout.getChildAt(0);
        TextView tv0 = (TextView)tableRow0.getChildAt(0);
        tv0.setText(trackLog.getName());
        tv0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        TableRow tableRow1 = (TableRow)tableLayout.getChildAt(1);
        TextView tv1_0 = (TextView)tableRow1.getChildAt(0);
        tv1_0.setText( (statistic.getSegmentIdx()<0)?"All":""+statistic.getSegmentIdx() );
        TextView tv1_1 = (TextView)tableRow1.getChildAt(1);
        tv1_1.setText( sdf1.format(statistic.getTStart()) );
        TextView tv1_2 = (TextView)tableRow1.getChildAt(2);
        tv1_2.setText( sdf2.format(statistic.getTStart()) );
        TextView tv1_3 = (TextView)tableRow1.getChildAt(3);
        tv1_3.setText( statistic.durationToString() );
        setDrawable(tv1_3, R.drawable.duration);
        TextView tv1_4 = (TextView)tableRow1.getChildAt(4);
        tv1_4.setText( String.format(Locale.ENGLISH,"%.2fkm",statistic.getTotalLength()/1000.0));
        setDrawable(tv1_4, R.drawable.length);

        TableRow tableRow2 = (TableRow)tableLayout.getChildAt(2);
        TextView tv2_0 = (TextView)tableRow2.getChildAt(0);
        tv2_0.setText( String.format(Locale.ENGLISH,"%d",statistic.getNumPoints()));
        TextView tv2_1 = (TextView)tableRow2.getChildAt(1);
        tv2_1.setText( String.format(Locale.ENGLISH,"%.1fm",statistic.getGain()));
        setDrawable(tv2_1, R.drawable.gain);
        TextView tv2_2 = (TextView)tableRow2.getChildAt(2);
        tv2_2.setText( String.format(Locale.ENGLISH,"%.1fm",statistic.getLoss()));
        setDrawable(tv2_2, R.drawable.loss);
        TextView tv2_3 = (TextView)tableRow2.getChildAt(3);
        String sMaxEle =  (Math.abs(statistic.getMaxEle())<(Math.abs(PointModel.NO_ELE)))?String.format(Locale.ENGLISH,"%.1fm",statistic.getMaxEle()):"";
        tv2_3.setText( sMaxEle );
        setDrawable(tv2_3, R.drawable.maxele);
        TextView tv2_4 = (TextView)tableRow2.getChildAt(4);
        String sMinEle =  (Math.abs(statistic.getMinEle())<(Math.abs(PointModel.NO_ELE)))?String.format(Locale.ENGLISH,"%.1fm",statistic.getMinEle()):"";
        tv2_4.setText( sMinEle );
        setDrawable(tv2_4, R.drawable.minele);

        setViewtreeColor(tableLayout, colorId);


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

    private void setDrawable(TextView textView, int resourceID){
        Drawable drawable = resources.getDrawable( resourceID, getTheme());
        drawable.setBounds(0,0,40,40);
        textView.setCompoundDrawables(drawable, null, null, null);
    }


    @Override
    protected void onStop() {
        super.onStop();

        LinearLayout parent = findViewById(R.id.trackView);
        parent.removeAllViews();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("MGMapViewer:");
        if (v instanceof TableLayout){
            menu.add(0, v.getId(),1,"Delete Track");
            TrackLogStatistic statistic = selectionRefs.get(v.getId()).getTrackLog().getTrackStatistic();
            if ((statistic.getMinEle() != TrackLogPoint.NO_ELE) && (statistic.getMaxEle() != TrackLogPoint.NO_ELE)){
                menu.add(0, v.getId(),0,"Height Profile");
            }
            menu.add(0, v.getId(),0,"Select Track");
            menu.add(0, v.getId(),0,"Share Track");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        TrackLog trackLog = selectionRefs.get(item.getItemId()).getTrackLog();
        if (item.getTitle().equals("Delete Track")) {
            Log.i(MGMapApplication.LABEL, NameUtil.context() +" Delete Track " + trackLog.getName());
            application.metaTrackLogs.remove(trackLog);
            application.availableTrackLogsObservable.availableTrackLogs.remove(trackLog);
            if (application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() == trackLog) {
                application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRef());
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Delete Track");
            builder.setMessage(trackLog.getName());

            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " delete track "+trackLog.getName() );
                    PersistenceManager.getInstance().deleteTrack(trackLog.getName());
                    AndroidUtil.restartActivity(TrackStatisticActivity.this);
                }
            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " abort delete track "+trackLog.getName() );
                }
            });

            AlertDialog alert = builder.create();
            alert.show();

        }
        if (item.getTitle().equals("Select Track")){
            Log.i(MGMapApplication.LABEL, NameUtil.context() +" Select Track " + trackLog.getName());
            application.lastPositionsObservable.clear();
            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRefZoom(trackLog, -1, true));
            application.availableTrackLogsObservable.changed();
            finish();
        }
        if (item.getTitle().equals("Share Track")){
            Log.i(MGMapApplication.LABEL, NameUtil.context() +" Share Track " + trackLog.getName());

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("*/*");
            sendIntent.putExtra(Intent.EXTRA_STREAM, PersistenceManager.getInstance().getGpxUri(trackLog.getName()));
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sendIntent, "Share "+trackLog.getName()+".gpx to ..."));
        }
        if (item.getTitle().equals("Height Profile")){
            Log.i(MGMapApplication.LABEL, NameUtil.context() +" Height Profile Track " + trackLog.getName());
            application.availableTrackLogsObservable.setSelectedTrackLogRef(new TrackLogRefZoom(trackLog, -1, true));
            application.availableTrackLogsObservable.changed();
            Intent intent = new Intent(this, HeightProfileActivity.class);
            startActivity(intent);
        }


        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}

