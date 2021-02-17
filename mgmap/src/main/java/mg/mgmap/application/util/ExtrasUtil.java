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
package mg.mgmap.application.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.util.gpx.GpxImporter;

/** Utility for extra tasks */
public class ExtrasUtil {


    public static void checkCreateMeta(PersistenceManager persistenceManager, MetaDataUtil metaDataUtil, AltitudeProvider altitudeProvider){
        Log.i(MGMapApplication.LABEL, NameUtil.context() );
        try {
            List<String> gpxNames = persistenceManager.getGpxNames();
            List<String> metaNames = persistenceManager.getMetaNames();

            List<String> newGpxNames = new ArrayList<>(gpxNames); // create  meta files for new gpx
            newGpxNames.removeAll(metaNames);
            metaNames.removeAll(gpxNames); // remove meta files without corresponding gpx

            for (String name : newGpxNames){
                Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Create meta file for "+name );
                TrackLog trackLog = new GpxImporter(altitudeProvider).parseTrackLog(name, persistenceManager.openGpxInput(name));
                metaDataUtil.createMetaData(trackLog);
                metaDataUtil.writeMetaData(PersistenceManager.getInstance().openMetaOutput(name), trackLog);
            }
            for (String name : metaNames){
                Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Delete meta file for "+name);
                PersistenceManager.getInstance().deleteTrack(name);
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }
}
