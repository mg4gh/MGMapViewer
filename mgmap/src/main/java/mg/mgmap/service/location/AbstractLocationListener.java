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
package mg.mgmap.service.location;

import android.location.Location;
import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.application.util.AltitudeProvider;
import mg.mgmap.application.util.GeoidProvider;
import mg.mgmap.generic.util.basic.NameUtil;

/**
 * Location Listener for TrackLoggerService
 */

public class AbstractLocationListener {

    protected static final float ACCURACY_LIMIT = 30.0f; // accuracy limit in meter
    protected final AltitudeProvider altitudeProvider;
    protected final GeoidProvider geoidProvider;
    protected final TrackLoggerService trackLoggerService;


    AbstractLocationListener(MGMapApplication application, TrackLoggerService trackLoggerService){
        this.trackLoggerService = trackLoggerService;
        altitudeProvider = application.getAltitudeProvider();
        geoidProvider = application.getGeoidProvider();
    }

    protected void locationChanged(Location location) {
        if ((location.hasAccuracy()) && (location.getAccuracy() < ACCURACY_LIMIT)){
            float altAccuracy = location.hasVerticalAccuracy()?location.getVerticalAccuracyMeters():0;
            double alt = location.hasAltitude()?location.getAltitude():0;
            float hgtAlt = altitudeProvider.getAltitude(location.getLatitude(), location.getLongitude());
            float geoidOffset = (alt==0)?0:geoidProvider.getGeoidOffset(location.getLatitude(), location.getLongitude());
            TrackLogPoint lp = TrackLogPoint.createGpsLogPoint(System.currentTimeMillis(), location.getLatitude(), location.getLongitude(),
                    location.getAccuracy(), alt, geoidOffset, altAccuracy, hgtAlt);
            trackLoggerService.onNewTrackLogPoint(lp);
        } else {
            Log.w(MGMapApplication.LABEL, NameUtil.context() + " location dropped hasacc="+location.hasAccuracy()+ " acc="+location.getAccuracy());
        }
    }


    protected void activate(int minMillis,int minDistance) throws SecurityException{
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" start locationListener ("+this.getClass().getSimpleName()+")");
    }


    protected void deactivate(){
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" stop locationListener ("+this.getClass().getSimpleName()+")");
    }

}
