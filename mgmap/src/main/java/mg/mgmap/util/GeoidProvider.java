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
package mg.mgmap.util;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides for a given point (latitude, longitude) a difference between wgs84 altitude and nmea altitude.
 * For performance issues the results are cached.
 */

public class GeoidProvider {

    private static final int START_DATA = 408;

    private InputStream is = null;
    private SparseArray<Float> ccache = new SparseArray<>();
    private AssetManager am;

    public GeoidProvider(Application application){
        am = application.getAssets();
    }



    public float getGeoidOffset(double latitude, double longitude){
        try {
            int latOffset = ((int) ((90 - latitude) * 4)) * 1440 * 2;
            int lonOffset = ((int) (longitude * 4)) * 2;
            Float cachedGeoid = ccache.get(latOffset+lonOffset);
            if (cachedGeoid != null){
                return cachedGeoid; // chache hits
            } else {
                synchronized (this){
                    InputStream is = am.open("nor/egm96-15.pgm");
                    int offset = START_DATA+latOffset + lonOffset;
                    if (is.skip(offset) == offset){
                        int b2 = is.read();
                        int b1 = is.read();
                        is.close();
                        int rawValue = ((b2 & 0xff) << 8) + (b1 & 0xff);
                        float geoid = rawValue * 0.003f - 108;
                        ccache.put(latOffset + lonOffset, geoid);
                        return geoid;
                    };
                }
            }
        } catch (IOException e) {
            Log.e(GeoidProvider.class.getSimpleName(),e.getMessage(),e);
        }
        return 0;
    }


}
