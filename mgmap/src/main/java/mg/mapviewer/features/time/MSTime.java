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
package mg.mapviewer.features.time;

import android.content.Context;
import android.os.BatteryManager;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;

public class MSTime extends MGMicroService {

    private PrefTextView ptvTime = null;
    private PrefTextView ptvBat = null;

    public MSTime(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    public PrefTextView initStatusLine(PrefTextView ptv, String info) {
        if (info.equals("time")){
            ptv.setPrefData(null, new int[]{R.drawable.duration2});
            ptv.setFormat(Formatter.FormatType.FORMAT_TIME);
            ptvTime = ptv;
        }
        if (info.equals("bat")){
            ptv.setPrefData(null, new int[]{R.drawable.bat});
            ptv.setFormat(Formatter.FormatType.FORMAT_INT);
            ptvBat = ptv;
        }
        return ptv;
    }

    @Override
    protected void start() {
        super.start();
        getTimer().postDelayed(timerTaskTime,1000);
    }

    @Override
    protected void stop() {
        super.stop();
        getTimer().removeCallbacks(timerTaskTime);
    }

    int batCnt = 1;
    private final Runnable timerTaskTime = new Runnable() {
        @Override
        public void run() {
//            getControlView().updateTvTime(System.currentTimeMillis());
            getControlView().setStatusLineValue(ptvTime, System.currentTimeMillis());
            getTimer().postDelayed(timerTaskTime,1000);

            if (--batCnt <= 0){
                batCnt = 60;
//                getControlView().updateTvBat(getBatteryPercent());
                getControlView().setStatusLineValue(ptvBat, getBatteryPercent());
            }
        }
    };

    private int getBatteryPercent(){
        BatteryManager bm = (BatteryManager) getActivity().getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

}
