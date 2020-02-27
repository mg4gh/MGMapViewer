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

import java.util.List;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.TrackLog;

/** Utility for extra tasks */
public class ExtrasUtil {



//    public static void generate(BoundingBox bb){
//        Log.i(MGMapApplication.LABEL, NameUtil.context() + " generate: Min- and Max-TileIndex values for BoundingBox " + bb);
//        int tileSize = 512;
//        for (byte zoomLevel=10; zoomLevel<= 16; zoomLevel++){
//            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
//            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bb.minLongitude , mapSize) , zoomLevel, tileSize);
//            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bb.maxLongitude , mapSize) , zoomLevel, tileSize);
//            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bb.maxLatitude , mapSize) , zoomLevel, tileSize); // min and max reversed for tiles
//            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bb.minLatitude , mapSize) , zoomLevel, tileSize);
//
//            Log.i(MGMapApplication.LABEL, NameUtil.context() +" "+String.format(Locale.ENGLISH,"dls %d %d %d %d %d",zoomLevel,tileXMin, tileXMax, tileYMin, tileYMax));
//        }
//    }

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
