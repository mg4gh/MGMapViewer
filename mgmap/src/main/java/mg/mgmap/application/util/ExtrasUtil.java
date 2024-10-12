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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxImporter;

/** Utility for extra tasks */
public class ExtrasUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static ArrayList<String> checkCreateMeta(PersistenceManager persistenceManager, MetaDataUtil metaDataUtil, ElevationProvider elevationProvider){
        mgLog.i();
        try {
            List<String> gpxNames = persistenceManager.getGpxNames();
            List<String> metaNames = persistenceManager.getMetaNames();

            ArrayList<String> newGpxNames = new ArrayList<>(gpxNames); // create  meta files for new gpx
            newGpxNames.removeAll(metaNames);
            metaNames.removeAll(gpxNames); // remove meta files without corresponding gpx

            for (String name : newGpxNames){
                mgLog.i("Create meta file for "+name );
                try (InputStream gpxIs = persistenceManager.openGpxInput(name)){
                    TrackLog trackLog = new GpxImporter(elevationProvider).parseTrackLog(name, gpxIs);
                    if (trackLog != null){
                        metaDataUtil.createMetaData(trackLog);
                        metaDataUtil.writeMetaData(persistenceManager.openMetaOutput(name), trackLog);
                    }
                }  catch (Exception e){ mgLog.e(e); }
            }
            for (String name : metaNames){
                mgLog.i("Delete meta file for "+name);
                persistenceManager.deleteTrack(name);
            }
            return newGpxNames;
        } catch (Exception e) {
            mgLog.e(e);
        }
        return new ArrayList<>();
    }
}
