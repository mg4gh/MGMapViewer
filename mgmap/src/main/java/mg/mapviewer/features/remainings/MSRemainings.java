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
package mg.mapviewer.features.remainings;

import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.pref.MGPref;

public class MSRemainings extends MGMicroService {


    public MSRemainings(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);
    private boolean bReverseRemainings = false;

   @Override
    protected void start() {
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
        getApplication().markerTrackLogObservable.addObserver(refreshObserver);
        getApplication().availableTrackLogsObservable.addObserver(refreshObserver);

        getControlView().tv_remain.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                bReverseRemainings = !bReverseRemainings;
                refreshObserver.update(null,null);
                return true;
            }
        });
    }

    @Override
    protected void stop() {
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
        getApplication().markerTrackLogObservable.deleteObserver(refreshObserver);
        getApplication().availableTrackLogsObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshRemainings();
            }
        });
    }

    /**
     * Prcondition: selectedTrackLog available and on of the following conditions:
     *    - MarkerTrackLog mit 1 Punkt, der in der Nähe des selectedTrackLog liegt
     *    - MarkerTrackLog mit 2 Punkten, die nahe des selectedTrackLog liegen
     *    - aktuelle Position ist Nahe dem selected Track
     */
    private void refreshRemainings(){
        TrackLog selectedTrackLog = getApplication().availableTrackLogsObservable.selectedTrackLogRef.getTrackLog();
        double dist = -1;
        int drawableId = 0;
        if (selectedTrackLog != null){
//            String text = "";

            TrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();
            if (mtl != null) {
                if (mtl.getNumberOfSegments() == 1) {
                    TrackLogSegment segment = mtl.getTrackLogSegment(0);
                    if (segment.size() == 1) {
                        TrackLogRefApproach ref1 = selectedTrackLog.getBestDistance(segment.get(0));
                        if (ref1 != null) {
                            dist = selectedTrackLog.getRemainingDistance(ref1);
                            drawableId = R.drawable.remaining;
                            if (bReverseRemainings) {
                                dist = selectedTrackLog.getTrackLogSegment(ref1.getSegmentIdx()).getStatistic().getTotalLength() - dist;
//                                dist = selectedTrackLog.getSegmentStatistics().get(ref1.getSegmentIdx()).getTotalLength() - dist;
                                drawableId = R.drawable.remaining2;
                            }
//                            text = String.format(Locale.ENGLISH, " %.2f km", dist / 1000.0);
                        }
                    }
                    if (segment.size() == 2) {
                        TrackLogRefApproach ref1 = selectedTrackLog.getBestDistance(segment.get(0));
                        TrackLogRefApproach ref2 = selectedTrackLog.getBestDistance(segment.get(1));
                        if ((ref1 != null) && (ref2 != null)) {
                            dist = selectedTrackLog.getRemainingDistance(ref1, ref2);
                            drawableId = R.drawable.remaining3;
//                            text = String.format(Locale.ENGLISH, " %.2f km", dist / 1000.0);
                        }
                    }
                }
            }
            if (drawableId == 0) {
                PointModel lp = getApplication().lastPositionsObservable.lastGpsPoint;
                if (prefGps.getValue() && (lp != null)) {
                    TrackLogRefApproach ref1 = selectedTrackLog.getBestDistance(lp);
                    if (ref1 != null) {
                        dist = selectedTrackLog.getRemainingDistance(ref1) ;
                        drawableId = R.drawable.remaining;
                        if (bReverseRemainings) {
                            double total = selectedTrackLog.getTrackLogSegment(ref1.getSegmentIdx()).getStatistic().getTotalLength();
                            dist = total - dist;
                            drawableId = R.drawable.remaining2;
                        }
//                        text = String.format(Locale.ENGLISH, " %.2f km", dist);
                    }
                }
            }
        }
        getControlView().updateRemainingForSelected(drawableId, dist);
    }

}
