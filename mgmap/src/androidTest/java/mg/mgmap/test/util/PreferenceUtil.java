package mg.mgmap.test.util;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.invoke.MethodHandles;
import java.util.List;

import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

public class PreferenceUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static void setPreference(AppCompatActivity settingsActivity, int keyId, String valueToSet){
        dismissPreferenceDialogFragmentCompat(settingsActivity);

        if (keyId != 0){
            String key = settingsActivity.getResources().getString(keyId);
            Fragment f = settingsActivity.getSupportFragmentManager().getFragments().get(0);
            if (f instanceof PreferenceFragmentCompat pfc) { // corresponds to preference screen
                Preference preference = pfc.findPreference(key);
                assert preference != null;
                if (preference instanceof ListPreference listPreference) {
                    settingsActivity.runOnUiThread(()->listPreference.setValue(valueToSet));
                } else if (preference instanceof EditTextPreference textPreference) {
                    settingsActivity.runOnUiThread(()->textPreference.setText(valueToSet));
                }
            }
        }
    }

    public static void dismissPreferenceDialogFragmentCompat(FragmentActivity activity) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof PreferenceDialogFragmentCompat) {
                ((PreferenceDialogFragmentCompat)fragment).dismiss();
            }
        }
    }


    public static BaseTestCase.PointOfView getPreferenceCenter(AppCompatActivity settingsActivity, int  keyId) {
        String key = settingsActivity.getResources().getString(keyId);

        Fragment f = settingsActivity.getSupportFragmentManager().getFragments().get(0);
        if (f instanceof PreferenceFragmentCompat pfc) { // corresponds to preference screen

            RecyclerView rv = pfc.getListView();
            RecyclerView.Adapter<?> ra = rv.getAdapter();

            if (ra instanceof PreferenceGroupAdapter pga) {
                @SuppressLint("RestrictedApi") int pIdx = pga.getPreferenceAdapterPosition(key);
                RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(pIdx);
                if (vh != null){
                    View v = vh.itemView;
                    int[] loc1 = new int[2];
                    v.getLocationOnScreen(loc1);
                    Point pt = new Point(loc1[0] + v.getWidth() / 2, loc1[1] + v.getHeight() / 2 );
                    mgLog.d(pt);
                    return new BaseTestCase.PointOfView(pt, v);
                }
            }
        }
        return null;
    }

}
