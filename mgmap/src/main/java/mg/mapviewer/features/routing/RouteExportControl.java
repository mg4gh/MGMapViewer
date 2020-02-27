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
package mg.mapviewer.features.routing;

import android.util.Log;
import android.view.View;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.GpxExporter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;

public class RouteExportControl extends Control {

    MSRouting msRouting;

    public RouteExportControl(MSRouting msRouting){
        super(true);
        this.msRouting = msRouting;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();

        TrackLog trackLog = msRouting.calcRouteTrackLog(mtl);
        try {
            String oldName = trackLog.getName().split("__")[0];
            if (PersistenceManager.getInstance().existsGpx(oldName)){
//                        PersistenceManager.getInstance().deleteTrack(oldName);
            }
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e );
        }
        GpxExporter.export(trackLog);
        try {
            application.metaTrackLogs.add(trackLog);
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

        v.setEnabled( (mtl != null) && application.showRouting.getValue() );
        setText(v, controlView.rstring(R.string.btRouteExport) );
    }
}
