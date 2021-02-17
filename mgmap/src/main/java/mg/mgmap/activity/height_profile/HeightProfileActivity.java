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
package mg.mgmap.activity.height_profile;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.PointModelUtil;

import java.util.ArrayList;
import java.util.Locale;

/**
 * An Activity to show the height profile of a given {@link mg.mgmap.generic.model.TrackLog}
 */
public class HeightProfileActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.height_profile_activity);

    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" started");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        boolean showAscentGraph = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_hprof_gl_key), false);
        showGraph(showAscentGraph);
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" finished");
    }

    public static boolean check4trackLogRef(MGMapApplication application){
        if (null != application.recordingTrackLogObservable.getTrackLog()) return true;
        if (null != application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog()) return true;
        return (null != application.routeTrackLogObservable.getTrackLog());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" started");
        GraphView graph = findViewById(R.id.graph);
        graph.getSeries().clear();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" finished");
    }


    private void showGraph(boolean showAscentGraph) {
        GraphView graph = findViewById(R.id.graph);
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
        if (showAscentGraph){
            graph.getSecondScale().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        // show normal x values
                        return String.format(Locale.ENGLISH,"%.1fkm",value/1000.0);
                    } else {
                        return String.format(Locale.GERMANY, "%.1f", (value)/100 )+"%";
                    }
                }
            });
            graph.getSecondScale().setMinY(-2000);
            graph.getSecondScale().setMaxY( 2000);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true);

        MGMapApplication application = (MGMapApplication)getApplication();
        createSeries(graph,  application.recordingTrackLogObservable.getTrackLog(), R.color.RED, showAscentGraph);
        createSeries(graph,  application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog(), R.color.BLUE, showAscentGraph);
        createSeries(graph,  application.routeTrackLogObservable.getTrackLog(), R.color.PURPLE_A150, showAscentGraph);
    }


    private void createSeries(GraphView graph, TrackLog trackLog, int colorId, boolean showAscentGraph){
        int color = getColor(colorId);
        ArrayList<SparseIntArray> segmentHeightProfiles = new ArrayList<>();
        ArrayList<SparseIntArray> segmentAscentProfiles = new ArrayList<>();
        fillHeightProfiles( trackLog,  segmentHeightProfiles, segmentAscentProfiles);
        createSeries(graph, segmentHeightProfiles, color, false);
        if (showAscentGraph){
            createSeries(graph, segmentAscentProfiles, color, true);
        }
    }


    private void createSeries(GraphView graph, ArrayList<SparseIntArray> arrays, int color, boolean secondScale){
        for (SparseIntArray array : arrays){
            if (array.size() < 2) continue;

            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setColor(color);
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            if (secondScale){
                graph.getSecondScale().addSeries(series);
                paint.setStrokeWidth(2);
            } else {
                graph.addSeries(series);
                paint.setStrokeWidth(8);
            }
            series.setCustomPaint(paint);

            for (int i=0; i<array.size(); i++){
                int distance = array.keyAt(i);
                int height = array.valueAt(i);

                series.appendData(new DataPoint(distance,height),false,100000);
            }

            graph.getViewport().setMinX(0);
            double maxX = Math.max(graph.getViewport().getMaxX(true), array.keyAt(array.size()-1));
            graph.getViewport().setMaxX(maxX);
        }
    }



    private void fillHeightProfiles(TrackLog trackLog, ArrayList<SparseIntArray> segmentHeightProfiles, ArrayList<SparseIntArray> segmentAscentProfiles){
        if (trackLog == null) return;

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
                fillHeightProfiles(trackLog.getTrackLogSegments(), segmentHeightProfiles, segmentAscentProfiles);
            }
        }
    }

    private void fillHeightProfiles(ArrayList<? extends MultiPointModel> mpms, ArrayList<SparseIntArray> segmentHeightProfiles, ArrayList<SparseIntArray> segmentAscentProfiles){
        double distance = 0d;
        for (int i = 0; i< mpms.size(); i++){
            SparseIntArray segmentHeightProfile = new SparseIntArray();
            SparseIntArray segmentAscentProfile = new SparseIntArray();
            distance = addSegmentHeightProfiles(distance,mpms.get(i), segmentHeightProfile, segmentAscentProfile );
            segmentHeightProfiles.add(segmentHeightProfile);
            segmentAscentProfiles.add(segmentAscentProfile);
        }
    }


    private double addSegmentHeightProfiles(double distance, MultiPointModel segment, SparseIntArray segmentHeightProfile, SparseIntArray segmentAscentProfile){
        if (segment.size() > 0){
            PointModel lastTlp = segment.get(0);
            PointModel lastPM = segment.get(0);
            segmentHeightProfile.put( (int)distance, (int)(lastPM.getEleA()*1000));
            SparseIntArray segmentAscentProfileRaw = new SparseIntArray();
            double deltaDistance = 0;
            for (PointModel pm : segment){
                deltaDistance += PointModelUtil.distance(pm, lastPM);
                lastPM = pm;
                if (deltaDistance > 150.0){
                    if ((pm.getEleA() != PointModel.NO_ELE) && (lastTlp.getEleA() != PointModel.NO_ELE)){
                        double deltaHeight = (pm.getEleD() - lastTlp.getEleD())*1000;
                        double glValue = deltaHeight / deltaDistance;
                        segmentAscentProfileRaw.put( (int)(distance+deltaDistance/2), (int)(glValue * 10));
                    }
                } else {
                    continue;
                }
                distance += deltaDistance;
                deltaDistance = 0;
                if (pm.getEleA() != PointModel.NO_ELE){
                    segmentHeightProfile.put( (int)distance, (int)(pm.getEleA()*1000));
                }
                lastTlp = pm;
            }
            segmentHeightProfile.put( (int)(distance+deltaDistance), (int)(lastPM.getEleA()*1000));

            for (int i=1; i< segmentAscentProfileRaw.size()-1; i++){
                int gl0 = segmentAscentProfileRaw.valueAt(i-1);
                int gl1 = segmentAscentProfileRaw.valueAt(i);
                int gl2 = segmentAscentProfileRaw.valueAt(i+1);
                int p0 = segmentHeightProfile.keyAt(i-1);
                int p1 = segmentHeightProfile.keyAt(i);
                int p2 = segmentHeightProfile.keyAt(i+1);
                int p3 = segmentHeightProfile.keyAt(i+2);
                int d0 = p1-p0;
                int d1 = p2-p1;
                int d2 = p3-p2;
                double f0 = 0.3, f1 = 1, f2 = 0.3;
                double d = d0*f0 + d1*f1 + d2*f2;
                double  we0=d0*f0/d, we1=d1*f1/d, we2=d2*f2/d;
                int glValue= (int)(gl0*we0+ gl1*we1 + gl2*we2 );

                segmentAscentProfile.put(segmentAscentProfileRaw.keyAt(i), glValue);
            }

        }

        return distance;
    }


}
