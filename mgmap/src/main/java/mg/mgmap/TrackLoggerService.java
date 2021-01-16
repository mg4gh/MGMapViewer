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
package mg.mgmap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import mg.mgmap.features.routing.TurningInstructionService;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.util.PrefCache;

/**
 * Android Service that coordinates gps and barometer access
 */

public class TrackLoggerService extends Service {

    private MGMapApplication application = null;

    private LocationListener locationListener = null;
    private BarometerListener barometerListener = null;
    private TurningInstructionService turningInstructionService = null;
    private boolean active = false;
    private Notification notification = null;
    private PrefCache prefCache = null;
    private Pref<Boolean> prefGps;

    public static void setPressureAlt(TrackLogPoint lp){
        if (lp.getPressure() != PointModel.NO_PRES){
            lp.setPressureAlt( Math.round(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,  lp.getPressure())  ) );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = (MGMapApplication)getApplication();
        prefCache = new PrefCache(this);
        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);

        turningInstructionService = new TurningInstructionService(application, application, prefCache);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "MGMapViewer information channel",
                    NotificationManager.IMPORTANCE_LOW);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" importance: "+channel.getImportance());

            Intent intent = new Intent(getApplicationContext(), MGMapActivity.class);
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.mg2)
                    .setContentTitle("MGMapViewer")
                    .setContentText("Location Listener is running.")
                    .setContentIntent(    PendingIntent.getActivity(this.getApplicationContext(), 0, intent , PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSound(null)
                    .build();
        }


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        application = (MGMapApplication)getApplication();

        boolean shouldBeActive = prefGps.getValue();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" acitve="+active+" shouldBeActive="+shouldBeActive);
        if (!active && shouldBeActive){
            active = true;
            activateService();
        }
        if (active && !shouldBeActive){
            active = false;
            deactivateService();
        }
        return START_STICKY;
    }


    protected void activateService(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(1, notification);
            }
            barometerListener = new BarometerListener(application,  SensorManager.SENSOR_DELAY_FASTEST);
            locationListener = new LocationListener(getApplication()){
                @Override
                protected void onNewTrackLogPoint(TrackLogPoint lp) {
                    lp.setPressure( barometerListener.getPressure() );
                    setPressureAlt(lp);
                    application.logPoints2process.add(lp);
                    Log.v(MGMapApplication.LABEL, NameUtil.context()+" new TrackLogPoint: "+lp);
                    turningInstructionService.handleNewPoint(lp);
                }
            };
            locationListener.activate(4000,20);
            barometerListener.activate();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }
    protected void deactivateService(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }
            locationListener.deactivate();
            barometerListener.deactivate();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
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
