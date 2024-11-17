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

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.generic.util.EditPref;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.DialogView;


public class TrackStatisticFilterDialog {

    private final int dp2 = ControlView.dp(2);

    final TrackStatisticActivity trackStatisticActivity;

    public TrackStatisticFilterDialog(TrackStatisticActivity trackStatisticActivity) {
        this.trackStatisticActivity = trackStatisticActivity;
    }

    public void show(){
        Context context = trackStatisticActivity;
        TrackStatisticFilter filter = trackStatisticActivity.getMGMapApplication().getTrackStatisticFilter();

        TableLayout table_dialog = new TableLayout(context);
        table_dialog.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        table_dialog.setBackgroundColor(context.getColor(R.color.CC_GRAY200));
        table_dialog.setPadding(dp2,dp2,dp2,dp2);

        table_dialog.addView( new Entry<>(context, "Name Part(s)", filter.prefFilterNamePart, filter.prefFilterNamePartOn ));
        table_dialog.addView( new Entry<>(context, "Date min", filter.prefFilterTimeMin, filter.prefFilterTimeMinOn ));
        table_dialog.addView( new Entry<>(context, "Date max", filter.prefFilterTimeMax, filter.prefFilterTimeMaxOn ));
        table_dialog.addView( new Entry<>(context, "Length min (km)", filter.prefFilterLengthMin, filter.prefFilterLengthMinOn ));
        table_dialog.addView( new Entry<>(context, "Length max (km)", filter.prefFilterLengthMax, filter.prefFilterLengthMaxOn ));
        table_dialog.addView( new Entry<>(context, "Gain min (m)", filter.prefFilterGainMin, filter.prefFilterGainMinOn ));
        table_dialog.addView( new Entry<>(context, "Gain max (m)", filter.prefFilterGainMax, filter.prefFilterGainMaxOn ));

        DialogView dialogView = trackStatisticActivity.findViewById(R.id.dialog_parent);
        dialogView.lock(() -> dialogView
                .setTitle("Filter Options")
                .setContentView(table_dialog)
                .setPositive("OK", evt -> {
                    for (int i=0; i<table_dialog.getChildCount(); i++){
                        if (table_dialog.getChildAt(i) instanceof Entry<?> entry) {
                            entry.confirm();
                        }
                    }
                    trackStatisticActivity.prefFilterChanged.toggle();
                })
                .setNegative("Cancel", evt-> trackStatisticActivity.prefFilterOn.toggle())
                .show());
    }


    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    private TextView createTextView(ViewGroup parent, String text, float weight){
        TextView tv = new TextView(parent.getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(dp2,dp2,dp2,dp2);
        tv.setLayoutParams(params);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(5, 5, 5, 5);
        tv.setText(text);
        tv.setTextColor(parent.getContext().getColor(R.color.CC_BLACK));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        parent.addView(tv);
        tv.setBackgroundColor(parent.getContext().getColor(R.color.CC_GRAY240));
        return tv;
    }


    @SuppressWarnings("SameParameterValue")
    private <T> EditPref<T> createEditPref(ViewGroup parent, Pref<T> pref, float weight){
        EditPref<T> et = new EditPref<>(parent.getContext(), pref);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(dp2,dp2,dp2,dp2);
        et.setLayoutParams(params);
        et.setGravity(Gravity.CENTER);
        et.setPadding(5, 5, 5, 5);
        et.setTextColor(parent.getContext().getColor(R.color.CC_BLACK));
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        parent.addView(et);
        et.setBackgroundColor(parent.getContext().getColor(R.color.CC_GRAY240));
        return et;
    }

    @SuppressWarnings("SameParameterValue")
    private CheckBox createCheckBox(ViewGroup parent, Pref<Boolean> pref, float weight){
        CheckBox cb = new CheckBox(parent.getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(dp2,dp2,dp2,dp2);
        cb.setLayoutParams(params);
        cb.setGravity(Gravity.CENTER);
        cb.setPadding(5, 5, 5, 5);
        cb.setBackgroundColor(parent.getContext().getColor(R.color.CC_GRAY240));
        cb.setChecked(pref.getValue());
        parent.addView(cb);
        return cb;
    }



    private class Entry<T>  extends TableRow{

        final Context context;
        final EditPref<T> epPref;
        final CheckBox cbPref;
        final Pref<T> pref;
        final Pref<Boolean> prefOn;

        private Entry(Context context, String name, Pref<T> pref, Pref<Boolean> prefOn){
            super(context);
            this.context = context;
            this.pref = pref;
            this.prefOn = prefOn;

            setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            setBackgroundColor(context.getColor(R.color.CC_GRAY200));

            createTextView(this, name, 20);
            epPref = createEditPref(this, pref, 20);
            cbPref = createCheckBox(this, prefOn, 5);
            cbPref.setOnCheckedChangeListener((compoundButton, checked) -> {
                prefOn.setValue(checked);
                visualizeChecked(checked);
            });
            visualizeChecked(prefOn.getValue());
        }

        private void visualizeChecked(boolean checked){
            epPref.setEnabled(checked);
            epPref.setTextColor(context.getColor(checked?R.color.CC_BLACK : R.color.CC_GRAY100_A100));
        }

        private void confirm(){
            prefOn.setValue(cbPref.isChecked());
            epPref.confirm();
        }
    }

}
