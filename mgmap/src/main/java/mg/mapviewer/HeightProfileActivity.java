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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.GpxImporter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.PointModelUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * An Activity to show the height profile of a given {@link mg.mapviewer.model.TrackLog}
 */
public class HeightProfileActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.height_profile);

    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" started");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        showGraph();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" finished");
    }

    public static boolean check4trackLogRef(MGMapApplication application){
        if (null != application.recordingTrackLogObservable.getTrackLog()) return true;
        if (null != application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog()) return true;
        if (null != application.getMS(MSRouting.class).routingLineModel) return true;
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" started");
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.getSeries().clear();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" finished");
    }


    private void showGraph() {

        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.setBackgroundColor(0xFFAAAAAA);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return String.format(Locale.ENGLISH,"%.1fkm",value/1000.0);
                } else {
                    return String.format(Locale.GERMANY, "%dm", (int)((value) / 1000));
                }
            }
        });
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);

        MGMapApplication application = (MGMapApplication)getApplication();
        TrackLog rtl = application.recordingTrackLogObservable.getTrackLog();
        if (rtl != null)  createSeries(graph, getHeightProfile(rtl), 0xFFFF0000);

        TrackLog stl = application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog();
        if (stl != null)  createSeries(graph, getHeightProfile(stl), 0xFF0000FF);

        ArrayList<MultiPointModel> rlm = application.getMS(MSRouting.class).routingLineModel;
        if (rlm != null)  createSeries(graph, getHeightProfile(rlm), 0x96C800E6);

//
//        if (graph.getSeries().isEmpty()) {
//            Log.i(MGMapApplication.LABEL, NameUtil.context() +" create line graph series");
//            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
//            graph.addSeries(series);
//            series.setColor(0xFFFF0000);
//        }
//        LineGraphSeries<DataPoint> series = (LineGraphSeries<DataPoint>) graph.getSeries().get(0);
//
//        SparseIntArray array = trackLog.getHeightProfile(idx);
//        if (array.size() < 2){
////            MGMapActivity activity = controlView.getActivity();
//            Intent intent = new Intent(this, MGMapActivity.class);
//            this.startActivity(intent);
//        }
//        for (int i=0; i<array.size(); i++){
//            int distance = array.keyAt(i);
//            int height = array.valueAt(i);
//
//            series.appendData(new DataPoint(distance,height),false,100000);
//        }
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(array.keyAt(array.size()-1));
    }


    private void createSeries(GraphView graph, SparseIntArray array, int color){
        if (array.size() < 2) return;

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        graph.addSeries(series);
        series.setColor(color);

        for (int i=0; i<array.size(); i++){
            int distance = array.keyAt(i);
            int height = array.valueAt(i);

            series.appendData(new DataPoint(distance,height),false,100000);
        }

        graph.getViewport().setMinX(0);
        double maxX = Math.max(graph.getViewport().getMaxX(true), array.keyAt(array.size()-1));
        graph.getViewport().setMaxX(maxX);
    }



    private SparseIntArray getHeightProfile(TrackLog trackLog){
        TrackLogSegment tls = null;
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            if (segment.getStatistic().getNumPoints() >= 2){
                tls = segment;
                break;
            }
        }
        if (tls != null){
            if ((tls.get(0).getEleA() != PointModel.NO_ELE) && (tls.get(1).getEleA() != PointModel.NO_ELE)){
                // ok Tracklog seems to have ele values
                return getHeightProfile(trackLog.asMPMList());
            } else {
                // the reason for no ele values is probably, that the track is loaded via lalo meta data - thus it doesn't contain ele values - try to load corresponding gpx

                try {
                    InputStream fis =  PersistenceManager.getInstance().openGpxInput(trackLog.getName());
                    if (fis != null){
                        TrackLog trackLogGpx = new GpxImporter().parseTrackLog(trackLog.getName(), fis);
                        return getHeightProfile(trackLogGpx.asMPMList());
                    }
                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                }
            }
        }
        return null;
    }

    private SparseIntArray getHeightProfile(ArrayList<MultiPointModel> mpms){
        double distance = 0d;
        SparseIntArray array = new SparseIntArray();
        for (int i = 0; i< mpms.size(); i++){
            distance = addSegmentHeightProfile(distance,array,mpms.get(i) );
        }
        return array;
    }


    private double addSegmentHeightProfile(double distance, SparseIntArray array, MultiPointModel segment){
        if (segment.size() > 0){
            PointModel lastTlp = segment.get(0);
            for (PointModel pm : segment){
                distance += PointModelUtil.distance(pm, lastTlp);
                if (pm.getEleA() != PointModel.NO_ELE){
                    array.put( (int)distance, (int)(pm.getEleA()*1000));
                }
                lastTlp = pm;
            }
        }
        return distance;
    }


}
