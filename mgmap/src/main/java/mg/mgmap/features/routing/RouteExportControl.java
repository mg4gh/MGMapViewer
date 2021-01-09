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
package mg.mgmap.features.routing;

import android.util.Log;
import android.view.View;

import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.model.WriteableTrackLog;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogRef;
import mg.mgmap.model.TrackLogRefZoom;
import mg.mgmap.util.Control;
import mg.mgmap.util.GpxExporter;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.MGPref;

public class RouteExportControl extends Control {

    FSRouting msRouting;

    public RouteExportControl(FSRouting msRouting){
        super(true);
        this.msRouting = msRouting;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();

        TrackLog trackLog = application.routeTrackLogObservable.getTrackLog();
        GpxExporter.export(trackLog);
        try {
            TrackLogRef refSelected = new TrackLogRefZoom(trackLog,trackLog.getNumberOfSegments()-1,false);
            application.availableTrackLogsObservable.setSelectedTrackLogRef(refSelected);
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e );
        }

    }

    @Override
    public void onPrepare(View v) {
        MGMapApplication application = controlView.getApplication();
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = application.routeTrackLogObservable.getTrackLog();

        v.setEnabled( (rotl != null) && (mtl.getTrackStatistic().getNumPoints() > 1) && (MGPref.get(R.string.FSRouting_pref_alphaRoTL,1.0f).getValue() > 0.25));
        setText(v, controlView.rstring(R.string.btRoTSave) );
    }
}
