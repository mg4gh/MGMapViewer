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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Location Listener for TrackLoggerService
 */

public class GnssLocationListener extends AbstractLocationListener {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final LocationManager locationManager;
    private final LocationListener locationListener;

    GnssLocationListener(MGMapApplication application, TrackLoggerService trackLoggerService){
        super(application, trackLoggerService);
        locationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationChanged(location);
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                mgLog.i("s="+s+" i="+i);
            }

            @Override
            public void onProviderEnabled(@NonNull String s) {
                mgLog.i(s);
            }

            @Override
            public void onProviderDisabled(@NonNull String s) {
                mgLog.i(s);
            }
        };
    }

    protected void activate(int minMillis,int minDistance) throws SecurityException{
        super.activate(minMillis, minDistance);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minMillis, minDistance, locationListener);
    }


    protected void deactivate(){
        locationManager.removeUpdates(locationListener);
        super.deactivate();
    }

}
