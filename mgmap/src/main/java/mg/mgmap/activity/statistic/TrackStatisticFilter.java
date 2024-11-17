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

import java.lang.invoke.MethodHandles;
import java.util.Calendar;

import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;

public class TrackStatisticFilter {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final PrefCache prefCache;
    final Pref<String> prefFilterNamePart;
    final Pref<Boolean> prefFilterNamePartOn;
    final Pref<Calendar> prefFilterTimeMin;
    final Pref<Boolean> prefFilterTimeMinOn;
    final Pref<Calendar> prefFilterTimeMax;
    final Pref<Boolean> prefFilterTimeMaxOn;
    final Pref<Float> prefFilterLengthMin;
    final Pref<Boolean> prefFilterLengthMinOn;
    final Pref<Float> prefFilterLengthMax;
    final Pref<Boolean> prefFilterLengthMaxOn;
    final Pref<Float> prefFilterGainMin;
    final Pref<Boolean> prefFilterGainMinOn;
    final Pref<Float> prefFilterGainMax;
    final Pref<Boolean> prefFilterGainMaxOn;

    public TrackStatisticFilter(PrefCache prefCache){
        this.prefCache = prefCache;

        prefFilterNamePart = prefCache.get(R.string.Statistic_pref_FilterNamePart, "");
        prefFilterNamePartOn = prefCache.get(R.string.Statistic_pref_FilterNamePartOn, false);
        Calendar calendarMin = Calendar.getInstance();
        calendarMin.set(2020, 0, 1);
        prefFilterTimeMin = prefCache.get(R.string.Statistic_pref_FilterTimeMin, calendarMin);
        prefFilterTimeMinOn = prefCache.get(R.string.Statistic_pref_FilterTimeMinOn, false);
        Calendar calendarMax = Calendar.getInstance();
        calendarMax.set(2022, 0, 1);
        prefFilterTimeMax = prefCache.get(R.string.Statistic_pref_FilterTimeMax, calendarMax);
        prefFilterTimeMaxOn = prefCache.get(R.string.Statistic_pref_FilterTimeMaxOn, false);
        prefFilterLengthMin = prefCache.get(R.string.Statistic_pref_FilterLengthMin, 20f);
        prefFilterLengthMinOn = prefCache.get(R.string.Statistic_pref_FilterLengthMinOn, false);
        prefFilterLengthMax = prefCache.get(R.string.Statistic_pref_FilterLengthMax, 50f);
        prefFilterLengthMaxOn = prefCache.get(R.string.Statistic_pref_FilterLengthMaxOn, false);
        prefFilterGainMin = prefCache.get(R.string.Statistic_pref_FilterGainMin, 500f);
        prefFilterGainMinOn = prefCache.get(R.string.Statistic_pref_FilterGainMinOn, false);
        prefFilterGainMax = prefCache.get(R.string.Statistic_pref_FilterGainMax, 1000f);
        prefFilterGainMaxOn = prefCache.get(R.string.Statistic_pref_FilterGainMaxOn, false);
    }




    public void checkFilter(TrackLog aTrackLog){
        boolean res = true;
        if (prefFilterNamePartOn.getValue()){
            String[] nameParts = prefFilterNamePart.getValue().split("\\s+");
            for (String part : nameParts){
                if (part.startsWith("!")){
                    res &= !(aTrackLog.getName().toUpperCase().contains(part.substring(1).toUpperCase()));
                } else {
                    res &= (aTrackLog.getName().toUpperCase().contains(part.toUpperCase()));
                }
            }
        }
        if (prefFilterTimeMinOn.getValue()){
            res &= (prefFilterTimeMin.getValue().getTimeInMillis() <= aTrackLog.getTrackStatistic().getTStart());
        }
        if (prefFilterTimeMaxOn.getValue()){
            res &= (prefFilterTimeMax.getValue().getTimeInMillis() >= aTrackLog.getTrackStatistic().getTStart());
        }
        if (prefFilterLengthMinOn.getValue()){
            mgLog.v(prefFilterLengthMin.getValue()+" "+aTrackLog.getTrackStatistic().getTotalLength()/1000);
            res &= (prefFilterLengthMin.getValue() <= aTrackLog.getTrackStatistic().getTotalLength()/1000); // length in Statistic is in m
        }
        if (prefFilterLengthMaxOn.getValue()){
            res &= (prefFilterLengthMax.getValue() >= aTrackLog.getTrackStatistic().getTotalLength()/1000); // length in Statistic is in m
        }
        if (prefFilterGainMinOn.getValue()){
            res &= (prefFilterGainMin.getValue() <= aTrackLog.getTrackStatistic().getGain());
        }
        if (prefFilterGainMaxOn.getValue()){
            res &= (prefFilterGainMax.getValue() >= aTrackLog.getTrackStatistic().getGain());
        }
        aTrackLog.setFilterMatched(res);
    }

}
