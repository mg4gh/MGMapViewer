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
package mg.mapviewer.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.TrackLog;

/** Utility for extra tasks */
public class ExtrasUtil {


    public static void checkCreateMeta(){
        Log.i(MGMapApplication.LABEL, NameUtil.context() );

        try {
            List<String> candidates = PersistenceManager.getInstance().getGpxNames();
            candidates.removeAll( PersistenceManager.getInstance().getMetaNames() );

            for (String name : candidates){
                TrackLog trackLog = new GpxImporter().parseTrackLog(name, PersistenceManager.getInstance().openGpxInput(name));
                MetaDataUtil.createMetaData(trackLog);
                MetaDataUtil.writeMetaData(PersistenceManager.getInstance().openMetaOutput(name), trackLog);

            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }
}
