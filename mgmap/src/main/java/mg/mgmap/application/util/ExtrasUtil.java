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

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxImporter;

/** Utility for extra tasks */
public class ExtrasUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static ArrayList<String> checkCreateMeta(MGMapApplication mgMapApplication, UUID currentRun){
        mgLog.i("checkCreateMeta started  - currentRun="+currentRun);
        try {
            PersistenceManager persistenceManager = mgMapApplication.getPersistenceManager();
            MetaDataUtil metaDataUtil = mgMapApplication.getMetaDataUtil();

            TreeSet<String> gpxNames = new TreeSet<>(Collections.reverseOrder());
            gpxNames.addAll( persistenceManager.getGpxNames() );
            List<String> metaNames = persistenceManager.getMetaNames();

            ArrayList<String> newGpxNames = new ArrayList<>(gpxNames); // create  meta files for new gpx
            newGpxNames.removeAll(metaNames);
            metaNames.removeAll(gpxNames); // remove meta files without corresponding gpx

            int cntMetaLoaded = 0, cntMetaCreated = 0;
            TrackLog trackLog = null;
            for (String name : gpxNames){
                if (persistenceManager.isGpxOlderThanMeta(name)) {
                    cntMetaLoaded++;
                    trackLog = new TrackLog();
                    trackLog.setName(name);
                    mgMapApplication.getMetaDataUtil().readMetaData(persistenceManager.openMetaInput(name), trackLog);
                } else {
                    cntMetaCreated++;
                    mgLog.d("Create meta file for "+name );
                    try (InputStream gpxIs = persistenceManager.openGpxInput(name)){
                        trackLog = new GpxImporter(mgMapApplication.getElevationProvider()).parseTrackLog(name, gpxIs);
                        if (trackLog != null){
                            trackLog.setModified(false);
                            metaDataUtil.createMetaData(trackLog);
                            metaDataUtil.writeMetaData(persistenceManager.openMetaOutput(name), trackLog);
                        }
                    }  catch (Exception e){ mgLog.e(e); }
                }
                if (trackLog != null){ // might be null, if gpx is corrupted
                    if (mgMapApplication.currentRun != currentRun) break; // check correct run immediately before adding to metaData
                    mgMapApplication.addMetaDataTrackLog(trackLog);
                }
            }
            mgLog.i(String.format(Locale.ENGLISH,"checkCreateMeta summary gpxNames=%d cntMetaLoaded=%d cntMetaCreated=%d",gpxNames.size(),cntMetaLoaded,cntMetaCreated));
            for (String name : metaNames){
                mgLog.d("Delete meta file for "+name);
                persistenceManager.deleteTrack(name);
            }
            mgLog.i("checkCreateMeta finished  - currentRun="+currentRun);
            return newGpxNames;
        } catch (Exception e) {
            mgLog.e(e);
        }
        return new ArrayList<>();
    }
}
