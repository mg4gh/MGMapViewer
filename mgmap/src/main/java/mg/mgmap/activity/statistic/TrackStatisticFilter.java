package mg.mgmap.activity.statistic;

import android.util.Log;

import java.util.Calendar;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.NameUtil;

public class TrackStatisticFilter {

    PrefCache prefCache;
    Pref<String> prefFilterNamePart;
    Pref<Boolean> prefFilterNamePartOn;
    Pref<Calendar> prefFilterTimeMin;
    Pref<Boolean> prefFilterTimeMinOn;
    Pref<Calendar> prefFilterTimeMax;
    Pref<Boolean> prefFilterTimeMaxOn;
    Pref<Float> prefFilterLengthMin;
    Pref<Boolean> prefFilterLengthMinOn;
    Pref<Float> prefFilterLengthMax;
    Pref<Boolean> prefFilterLengthMaxOn;
    Pref<Float> prefFilterGainMin;
    Pref<Boolean> prefFilterGainMinOn;
    Pref<Float> prefFilterGainMax;
    Pref<Boolean> prefFilterGainMaxOn;

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
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+prefFilterLengthMin.getValue()+" "+aTrackLog.getTrackStatistic().getTotalLength()/1000);
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
