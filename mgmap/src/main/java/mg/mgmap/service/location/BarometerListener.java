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

import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.NameUtil;

/**
 * Implements a BarometerListener on top of the generic SensorListener.
 *
 * Depending the the particular device there is some smoothing function observed.
 * Setting the speed to a high rate (short time) seems to disable unwanted smoothing of the pressure values.
 * Current assumptions is, that the smoothing works on a fix number of measured values, so the effect
 * of smoothing acts on a rather shot period of time, which is no problem for track recording.
 * TODO: speed as a configurable parameter.
 */
class BarometerListener implements SensorEventListener {


    private final SensorManager sensorManager;
    private final Sensor pressureSensor;

    private int cnt=0;
    private double lastPressure = PointModel.NO_PRES;
    private long lastEventTimeMillis = 0;

    private final int speed;
    private final LongSparseArray<Float> pValues = new LongSparseArray<>();
    private final long uptimeMillis;


    BarometerListener(MGMapApplication application, final int speed){
        this.speed = speed;
        sensorManager = (SensorManager) application.getSystemService(Application.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor != null){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " BarometerListener: "+"pressureSensor found");
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " isWakeupSensor="+pressureSensor.isWakeUpSensor());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " isAdditionalInfoSupported="+pressureSensor.isAdditionalInfoSupported());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " isDynamicSensor="+pressureSensor.isDynamicSensor());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " minDelay="+pressureSensor.getMinDelay());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " maxDelay="+pressureSensor.getMaxDelay());

            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " FifoMaxEventCount="+pressureSensor.getFifoMaxEventCount());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " ReportingMode="+pressureSensor.getReportingMode());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " MaximumRange="+pressureSensor.getMaximumRange());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " FifoReservedEventCount="+pressureSensor.getFifoReservedEventCount());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Power="+pressureSensor.getPower());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Resolution="+pressureSensor.getResolution());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(MGMapApplication.LABEL, NameUtil.context()+ " HighestDirectReportRateLevel="+pressureSensor.getHighestDirectReportRateLevel());
            }
        } else {
            Log.e(MGMapApplication.LABEL, NameUtil.context()+"BarometerListener: "+"pressureSensor not found");
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long currentTimeMillis = System.currentTimeMillis();
        uptimeMillis = (currentTimeMillis - elapsedRealtime);
    }

     @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        cnt++;
        float pressure = sensorEvent.values[0];
        long now = System.currentTimeMillis();
        long eventTimeMillis = ((sensorEvent.timestamp / 1000000) + uptimeMillis);
        synchronized (pValues){
            if ( eventTimeMillis/10 != lastEventTimeMillis/10){ // don't use event, if there was already another one in the same 1/100s
                lastEventTimeMillis = eventTimeMillis;
                pValues.append(eventTimeMillis, pressure);

                while (pValues.size() > 0){
                    long oldestKey = pValues.keyAt(0);
                    if ((eventTimeMillis - oldestKey) > 6000){ // keep pressure values for at most 6s
                        pValues.delete(oldestKey);
                    } else {
                        break;
                    }
                }
                Log.v(MGMapApplication.LABEL, NameUtil.context()+pressure+" ts="+now+" tse="+eventTimeMillis+" "+pValues.size());
            }
        }
    }

    float getPressure(double lat, double lon){ // parameters just for logging
        String logInfo = " ";
        synchronized (pValues){
            if (pValues.size() > 0){
                double total = 0;
                for (int idx=0; idx<pValues.size(); idx++){
                    total += pValues.valueAt(idx);
                }
                lastPressure = total/pValues.size();
                logInfo += "lastPressure="+lastPressure+" pValues.size()="+pValues.size();
            }
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+String.format(Locale.ENGLISH," lat=%2.6f lon=%2.6f ",lat, lon)+ logInfo);
        return (float)lastPressure;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.v(MGMapApplication.LABEL, NameUtil.context()+ "onAccuracyChanged: "+sensor.getName()+" "+i+" accChg");
    }

    void activate(){
        if (pressureSensor != null){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"activate: "+"start sensorEventListener");
            sensorManager.registerListener(this, pressureSensor, speed );
        }
    }
    void deactivate(){
        if (pressureSensor != null){
            sensorManager.unregisterListener(this);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"deactivate: "+"stop sensorEventListener");
            pValues.clear();
        }
    }

}
