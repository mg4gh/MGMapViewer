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
import android.os.SystemClock;
import android.util.LongSparseArray;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Implements a BarometerListener on top of the generic SensorListener.
 * Depending the the particular device there is some smoothing function observed.
 * Setting the speed to a high rate (short time) seems to disable unwanted smoothing of the pressure values.
 * Current assumptions is, that the smoothing works on a fix number of measured values, so the effect
 * of smoothing acts on a rather shot period of time, which is no problem for track recording.
 * TODO: speed as a configurable parameter.
 */
public class BarometerListener implements SensorEventListener {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final SensorManager sensorManager;
    private final Sensor pressureSensor;

    private long lastEventTimeMillis = 0;

    private final int speed;
    private final long barometerSmoothingPeriod;
    private final LongSparseArray<Float> pValues = new LongSparseArray<>();
    private final long uptimeMillis;

    private static int[] escalationCnt = null;

    public static void setEscalationCnt(int[] escalationCnt) {
        BarometerListener.escalationCnt = escalationCnt;
    }

    public BarometerListener(MGMapApplication application, final int speed, long barometerSmoothingPeriod){
        this.speed = speed;
        this.barometerSmoothingPeriod = barometerSmoothingPeriod;
        sensorManager = (SensorManager) application.getSystemService(Application.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor != null){
            mgLog.i("BarometerListener: pressureSensor found");
            mgLog.d("isWakeupSensor="+pressureSensor.isWakeUpSensor());
            mgLog.d("isAdditionalInfoSupported="+pressureSensor.isAdditionalInfoSupported());
            mgLog.d("isDynamicSensor="+pressureSensor.isDynamicSensor());
            mgLog.d("minDelay="+pressureSensor.getMinDelay());
            mgLog.d("maxDelay="+pressureSensor.getMaxDelay());
            mgLog.d("FifoMaxEventCount="+pressureSensor.getFifoMaxEventCount());
            mgLog.d("ReportingMode="+pressureSensor.getReportingMode());
            mgLog.d("MaximumRange="+pressureSensor.getMaximumRange());
            mgLog.d("FifoReservedEventCount="+pressureSensor.getFifoReservedEventCount());
            mgLog.d("Power="+pressureSensor.getPower());
            mgLog.d("Resolution="+pressureSensor.getResolution());
            mgLog.d("HighestDirectReportRateLevel="+pressureSensor.getHighestDirectReportRateLevel());
        } else {
            mgLog.w("BarometerListener: "+"pressureSensor not found");
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long currentTimeMillis = System.currentTimeMillis();
        uptimeMillis = (currentTimeMillis - elapsedRealtime);
    }

     @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float pressure = sensorEvent.values[0];
        long now = System.currentTimeMillis();
        long eventTimeMillis = ((sensorEvent.timestamp / 1000000) + uptimeMillis);
        synchronized (pValues){
            if ( eventTimeMillis/10 != lastEventTimeMillis/10){ // don't use event, if there was already another one in the same 1/100s
                lastEventTimeMillis = eventTimeMillis;
                pValues.append(eventTimeMillis, pressure);

                while (pValues.size() > 0){
                    long oldestKey = pValues.keyAt(0);
                    if ((eventTimeMillis - oldestKey) > barometerSmoothingPeriod){
                        pValues.delete(oldestKey);
                    } else {
                        break;
                    }
                }
                mgLog.v(pressure+" ts="+now+" tse="+eventTimeMillis+" "+pValues.size());
            }
        }
        int[] escalationCnt = BarometerListener.escalationCnt;
        if ((escalationCnt != null) && (escalationCnt.length >= 4)){
            escalationCnt[2]++;
            escalationCnt[3] = (int)(pressure*1000);
        }
    }

    void  providePressureData(TrackLogPoint tlp){
        synchronized (pValues){
            if (pValues.size() > 0){
                double total = 0;
                for (int idx=0; idx<pValues.size(); idx++){
                    total += pValues.valueAt(idx);
                }
                double avgPressure = total/pValues.size();
                tlp.setPressure((float)avgPressure);
                double totalDiff = 0;
                for (int idx=0; idx<pValues.size(); idx++){
                    totalDiff += Math.abs(pValues.valueAt(idx)-avgPressure);
                }
                double accPressure = totalDiff/pValues.size();
                tlp.setPressureAcc((float)accPressure);
                String logInfo = " avgPressure="+avgPressure+" accPressure="+accPressure+" pValues.size()="+pValues.size();
                mgLog.d(String.format(Locale.ENGLISH," lat=%2.6f lon=%2.6f ",tlp.getLat(), tlp.getLon())+ logInfo);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        mgLog.v("onAccuracyChanged: " + sensor.getName() + " " + i + " accChg");
    }

    void activate(){
        if (pressureSensor != null){
            mgLog.i("activate: "+"start sensorEventListener");
            sensorManager.registerListener(this, pressureSensor, speed );
        }
    }
    void deactivate(){
        if (pressureSensor != null){
            sensorManager.unregisterListener(this);
            mgLog.i("deactivate: "+"stop sensorEventListener");
            pValues.clear();
        }
    }

}
