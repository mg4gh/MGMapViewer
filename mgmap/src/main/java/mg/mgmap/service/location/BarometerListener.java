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
import android.util.Log;
import android.util.LongSparseArray;

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
    private float lastPressure = PointModel.NO_PRES;

    private final int speed;
    private final LongSparseArray<Float> pValues = new LongSparseArray<>();



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
    }

     @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        cnt++;
        float pressure = sensorEvent.values[0];
        long now = System.currentTimeMillis();
        synchronized (pValues){
            if (pValues.get(now) == null){
                pValues.append(now, pressure);

                while (true){
                    long oldestKey = pValues.keyAt(0);
                    if ((now - oldestKey) > 10000){ // keep pressure values for at most 10s
                        pValues.delete(oldestKey);
                    } else {
                        break;
                    }
                }
                Log.v(MGMapApplication.LABEL, NameUtil.context()+pressure+" "+pValues.size());
            }
        }
    }

    float getPressure(){
        synchronized (pValues){
            if (pValues.size() > 0){
                float total = 0;
                double maxAbsDiff = 0;
                double totalAbsDiff = 0;

                long tmax = pValues.keyAt(pValues.size()-1);
                long tmin = 0;
                for (int idx=pValues.size()-1; idx > 0; idx--){ // iterate backwards over the last pressure values
                    float pidx = pValues.valueAt(idx);
                    float plidx = pValues.valueAt(idx-1);
                    long tidx = pValues.keyAt(idx);
                    long tlidx = pValues.keyAt(idx-1);
                    total += pidx;
                    double tFactor = 1000.0 / (tidx - tlidx);
                    double pAbsDiff = Math.abs(plidx - pidx) * tFactor; // pressure diff per second
                    maxAbsDiff = Math.max(maxAbsDiff, pAbsDiff);
                    totalAbsDiff += pAbsDiff;

                    int cnt = pValues.size() - idx;

                    if ((tmax-tlidx > 500) && (tmax-tidx <= 500) && (cnt > 10)){
                        if ((maxAbsDiff < 5.0) && ( totalAbsDiff/cnt < 2)){  // if pressure measurements of the last 500ms has low variance, then just take the average
                            lastPressure = total / cnt;
                            tmin = tlidx;
                            break;
                        }
                    }
                    if ((tmax-tlidx > 2000) && (tmax-tidx <= 2000)){
                        if ((maxAbsDiff < 10.0) && ( totalAbsDiff/cnt < 5)){ // if pressure measurements of the last 2000ms has rather low variance, then take that average
                            lastPressure = total / cnt;
                            tmin = tlidx;
                            break;
                        }
                    }
                    if (idx == 1){ // else take the average of all (at most 10s) pressure data
                        lastPressure = total / cnt;
                    }
                }
                while (pValues.keyAt(0) < tmin){
                    pValues.delete(pValues.keyAt(0));
                }
            }
        }
        Log.d(MGMapApplication.LABEL, NameUtil.context()+lastPressure+" "+pValues.size());
        return lastPressure;
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
