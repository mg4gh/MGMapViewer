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

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.application.BaseConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.GeoidProvider;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.NameUtil;

/**
 * Location Listener for TrackLoggerService
 */

public class AbstractLocationListener {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected static final float ACCURACY_LIMIT = 30.0f; // accuracy limit in meter
    protected final ElevationProvider elevationProvider;
    protected final GeoidProvider geoidProvider;
    protected final TrackLoggerService trackLoggerService;
    private final PrefCache prefCache;
    private final BaseConfig baseConfig;

    AbstractLocationListener(MGMapApplication application, TrackLoggerService trackLoggerService){
        this.trackLoggerService = trackLoggerService;
        elevationProvider = application.getElevationProvider();
        geoidProvider = application.getGeoidProvider();
        prefCache = application.getPrefCache();
        baseConfig = application.baseConfig;
    }

    protected void locationChanged(Location location) {
        if ((location.hasAccuracy()) && (location.getAccuracy() < ACCURACY_LIMIT)){
            float wgs84eleAcc = location.hasVerticalAccuracy()?location.getVerticalAccuracyMeters(): PointModel.NO_ACC;
            double wgs84ele = location.hasAltitude()?location.getAltitude():PointModel.NO_ELE;
            float geoidOffset = (wgs84ele==PointModel.NO_ELE)?0:geoidProvider.getGeoidOffset(location.getLatitude(), location.getLongitude());
            TrackLogPoint lp = TrackLogPoint.createGpsLogPoint(System.currentTimeMillis(), location.getLatitude(), location.getLongitude(),
                    location.getAccuracy(), wgs84ele, geoidOffset, wgs84eleAcc);
            elevationProvider.setElevation(lp);
            double heightConsistencyThreshold;
            try {
                heightConsistencyThreshold = Double.parseDouble( prefCache.get(R.string.preferences_height_consistency_check_key, "100").getValue() );
            } catch (NumberFormatException e) {
                mgLog.e(e);
                heightConsistencyThreshold = 100;
            }
            if ((lp.getNmeaEle() == PointModel.NO_ELE) || (lp.getHgtEle() == PointModel.NO_ELE) || (Math.abs(lp.getNmeaEle() - lp.getHgtEle()) < heightConsistencyThreshold )){
                if (baseConfig.getMode() == BaseConfig.Mode.NORMAL){
                    trackLoggerService.onNewTrackLogPoint(lp);
                }
            }
        } else {
            mgLog.w("location dropped hasacc="+location.hasAccuracy()+ " acc="+location.getAccuracy());
        }
    }


    protected void activate(int minMillis,int minDistance) throws SecurityException{
        mgLog.i("start locationListener ("+this.getClass().getSimpleName()+")");
    }


    protected void deactivate(){
        mgLog.i("stop locationListener ("+this.getClass().getSimpleName()+")");
    }

}
