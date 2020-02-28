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
package mg.mapviewer;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.util.NameUtil;

/**
 * Location Listener for TrackLoggerService
 */

abstract class LocationListener implements android.location.LocationListener {

    private LocationManager locationManager;

    LocationListener(Application application){
        locationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onLocationChanged(Location location) {
        TrackLogPoint lp = TrackLogPoint.createGpsLogPoint(location);
        onNewTrackLogPoint(lp);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    void activate(int minMillis,int minDistance) throws SecurityException{
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" start locationListener");
        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minMillis, minDistance, this);
    }


    void deactivate(){
        locationManager.removeUpdates(this);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" stop locationListener");
    }

    /** will be overwritten */
    protected abstract void onNewTrackLogPoint(TrackLogPoint lp);

}
