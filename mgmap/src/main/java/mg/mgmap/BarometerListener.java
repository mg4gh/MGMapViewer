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

import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import mg.mgmap.model.PointModel;
import mg.mgmap.util.NameUtil;

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


    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private MGMapApplication application;

    private static final String TAG = NameUtil.getTag();

    private int cnt=0;
    private float lastPressure = PointModel.NO_PRES;

    private int speed;

    float getPressure(){
        return lastPressure;
    }


    BarometerListener(MGMapApplication application, final int speed){
        this.application = application;
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
            Log.e(TAG, "BarometerListener: "+"pressureSensor not found");
        }
    }

     @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        cnt++;
        lastPressure = sensorEvent.values[0];
        application.pressure = lastPressure;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.v(TAG, "onAccuracyChanged: "+sensor.getName()+" "+i+" accChg");
    }

    void activate(){
        if (pressureSensor != null){
            Log.i(TAG, "activate: "+"start sensorEventListener");
            sensorManager.registerListener(this, pressureSensor, speed );
        }
    }
    void deactivate(){
        if (pressureSensor != null){
            sensorManager.unregisterListener(this);
            Log.i(TAG, "deactivate: "+"stop sensorEventListener");
            application.pressure = PointModel.NO_PRES;
        }
    }

}