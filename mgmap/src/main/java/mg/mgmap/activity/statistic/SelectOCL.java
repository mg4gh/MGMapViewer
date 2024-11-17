/*
 * Copyright 2017 - 2022 mg4gh
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
package mg.mgmap.activity.statistic;

import android.view.View;

import java.util.ArrayList;

import mg.mgmap.generic.model.TrackLog;

public class SelectOCL implements View.OnClickListener{
    final ArrayList<TrackLog> trackLogs;
    final boolean select;

    public SelectOCL(ArrayList<TrackLog> allVisibleEntries, boolean select){
        this.trackLogs = allVisibleEntries;
        this.select = select;
    }

    @Override
    public void onClick(View v) {
        setSelectedAll(select);
    }

    private void setSelectedAll(boolean selected){
        for (TrackLog trackLog : trackLogs){
            trackLog.getPrefSelected().setValue(selected);
        }
    }

}
