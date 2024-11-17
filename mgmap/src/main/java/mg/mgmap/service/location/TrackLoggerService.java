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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.BaseConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.TurningInstructionService;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;

/**
 * Android Service that coordinates gps and barometer access
 */

public class TrackLoggerService extends Service {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private MGMapApplication application = null;

    private AbstractLocationListener locationListener = null;
    private BarometerListener barometerListener = null;
    private TrackLogPointVerifier trackLogPointVerifier = null;
    private TurningInstructionService turningInstructionService = null;
    private Notification notification = null;
    private PrefCache prefCache = null;
    private Pref<Boolean> prefGps;

    public static void setPressureEle(TrackLogPoint lp){
        if (lp.getPressure() != PointModel.NO_PRES){
            float pressureEle = Math.round(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,  lp.getPressure()) * 10 ) / 10.0f;
            float pressureEle2 = Math.round(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,  lp.getPressure()+lp.getPressureAcc()) * 10 ) / 10.0f;
            lp.setPressureEle( pressureEle );
            lp.setPressureEleAcc( pressureEle - pressureEle2 );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = (MGMapApplication)getApplication();
        prefCache = new PrefCache(this);
        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);

        turningInstructionService = new TurningInstructionService(application, application, prefCache);

        String CHANNEL_ID = "MGMapViewer_TrackLoggerService_Notification";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "TrackLoggerService notification channel",
                NotificationManager.IMPORTANCE_LOW);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        mgLog.i("importance: "+channel.getImportance());

        Intent intent = new Intent(getApplicationContext(), MGMapActivity.class);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGMapViewer")
                .setContentText("Location Listener is running.")
                .setContentIntent(    PendingIntent.getActivity(this.getApplicationContext(), 0, intent , PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setSound(null)
                .build();


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mgLog.i("onStartCommand");
        application = (MGMapApplication)getApplication();

        if (prefGps.getValue()){
            application.prefGpsState.setValue(true); // store current state in application.prefGpsState - so application can check this state before calling startForegroundService
            activateService();
        } else {
            application.prefGpsState.setValue(false);
            deactivateService();
        }
        return START_STICKY;
    }


    protected void activateService(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, notification);
            }
            long barometerSmoothingPeriod = 6000; // default value

            try {
                String sBarometerSmoothingPeriod = prefCache.get(R.string.preferences_pressure_smoothing_gl_key, "6000").getValue();
                barometerSmoothingPeriod = Long.parseLong(sBarometerSmoothingPeriod);
            } catch (NumberFormatException e) {
                mgLog.e(e);
            }
            mgLog.i("activateService smoothingPeriod="+barometerSmoothingPeriod);
            barometerListener = new BarometerListener(application,  SensorManager.SENSOR_DELAY_FASTEST, barometerSmoothingPeriod);
            boolean prefFused = prefCache.get(R.string.FSPosition_pref_FusedLocationProvider, false).getValue();
            locationListener = prefFused?new FusedLocationListener(application, this):new GnssLocationListener(application, this);

            int minMillis = 4000;
            int minDistance = 20;
            trackLogPointVerifier = new TrackLogPointVerifier(application);
            try {
                minMillis = Integer.parseInt( prefCache.get(R.string.preferences_gnss_minMillis_key, ""+minMillis).getValue() );
            } catch (NumberFormatException e){
                mgLog.e(e.getMessage());
            }
            try {
                minDistance = Integer.parseInt( prefCache.get(R.string.preferences_gnss_minMeter_key, ""+minDistance).getValue() );
            } catch (NumberFormatException e){
                mgLog.e(e.getMessage());
            }
            locationListener.activate(minMillis, minDistance);
            barometerListener.activate();
        } catch (Exception e) {
            mgLog.e(e);
        }

    }
    protected void deactivateService(){
        try {
            mgLog.i("deactivateService");
            stopForeground(STOP_FOREGROUND_REMOVE);
            if (locationListener != null) locationListener.deactivate();
            if (barometerListener != null) barometerListener.deactivate();
            Intent intent = new Intent(this, TrackLoggerService.class);
            stopService(intent);
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    protected void onNewTrackLogPoint(TrackLogPoint lp) {
        if (application.baseConfig.mode() == BaseConfig.Mode.NORMAL){
            barometerListener.providePressureData(lp);
            setPressureEle(lp);
            if (trackLogPointVerifier.verify(lp)){
                mgLog.v("new TrackLogPoint: "+lp);
                application.logPoints2process.add(lp);
                turningInstructionService.handleNewPoint(lp);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        prefCache.cleanup();
        super.onDestroy();
    }
}
