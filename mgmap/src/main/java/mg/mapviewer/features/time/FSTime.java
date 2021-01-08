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
import mg.mapviewer.FeatureService;
import mg.mapviewer.R;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.view.ExtendedTextView;

public class FSTime extends FeatureService {

    private ExtendedTextView etvTime = null;
    private ExtendedTextView etvBat = null;

    public FSTime(MGMapActivity mmActivity) {
        super(mmActivity);
        ttRefreshTime = 1000;
    }

    @Override
    public ExtendedTextView initStatusLine(ExtendedTextView etv, String info) {
        super.initStatusLine(etv,info);
        if (info.equals("time")){
            etv.setData(R.drawable.duration2);
            etv.setFormat(Formatter.FormatType.FORMAT_TIME);
            etvTime = etv;
        }
        if (info.equals("bat")){
            etv.setData(R.drawable.bat);
            etv.setFormat(Formatter.FormatType.FORMAT_INT);
            etvBat = etv;
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        cancelRefresh();
        super.onPause();
    }

    private int batCnt = 1;

    @Override
    protected void doRefreshResumedUI() {
        getControlView().setStatusLineValue(etvTime, System.currentTimeMillis());
        refreshObserver.onChange();
        if (--batCnt <= 0){
            batCnt = 60;
            getControlView().setStatusLineValue(etvBat, getBatteryPercent());
        }
    }

    private int getBatteryPercent(){
        BatteryManager bm = (BatteryManager) getActivity().getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

}
