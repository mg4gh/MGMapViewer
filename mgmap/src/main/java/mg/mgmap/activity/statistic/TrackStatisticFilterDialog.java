package mg.mgmap.activity.statistic;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.Calendar;

import mg.mgmap.R;
import mg.mgmap.generic.util.EditPref;
import mg.mgmap.generic.util.Pref;


public class TrackStatisticFilterDialog {

    private class Entry<T>  extends TableRow{

        Context context;
        EditPref<T> epPref;
        CheckBox cbPref;
        Pref<T> pref;
        Pref<Boolean> prefOn;

        private Entry(Context context, String name, Pref<T> pref, Pref<Boolean> prefOn){
            super(context);
            this.context = context;
            this.pref = pref;
            this.prefOn = prefOn;

            setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            setBackgroundColor(context.getColor(R.color.GRAY200));

            createTextView(this, name, 20);
            epPref = createEditPref(this, pref, 20);
            cbPref = createCheckBox(this, prefOn, 5);
            cbPref.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    prefOn.setValue(checked);
                    visualizeChecked(checked);
                }
            });
            visualizeChecked(prefOn.getValue());
        }

        private void visualizeChecked(boolean checked){
            epPref.setEnabled(checked);
            epPref.setTextColor(context.getColor(checked?R.color.WHITE: R.color.GRAY100_A150));
        }

        private void confirm(){
            prefOn.setValue(cbPref.isChecked());
            epPref.confirm();
        }
    }

    AlertDialog alertDialog;
    public void show(Context context, TrackStatisticActivity trackStatisticActivity){

        TrackStatisticFilter filter = trackStatisticActivity.getMGMapApplication().getTrackStatisticFilter();

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context );
        Context dialogContext = builder.getContext();

        TableLayout table_dialog = new TableLayout(dialogContext);
        table_dialog.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        table_dialog.setBackgroundColor(context.getColor(R.color.GRAY200));


        {
            TableRow row = new TableRow(dialogContext);
            row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            row.setBackgroundColor(context.getColor(R.color.GRAY200));

            TextView tv = createTextView(row, "Filter Options", 20);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
            tv.setTypeface(null, Typeface.BOLD);
            table_dialog.addView(row);
        }

        table_dialog.addView( new Entry<>(context, "Name Part(s)", filter.prefFilterNamePart, filter.prefFilterNamePartOn ));
        table_dialog.addView( new Entry<>(context, "Date min", filter.prefFilterTimeMin, filter.prefFilterTimeMinOn ));
        table_dialog.addView( new Entry<>(context, "Date max", filter.prefFilterTimeMax, filter.prefFilterTimeMaxOn ));
        table_dialog.addView( new Entry<>(context, "Length min (km)", filter.prefFilterLengthMin, filter.prefFilterLengthMinOn ));
        table_dialog.addView( new Entry<>(context, "Length max (km)", filter.prefFilterLengthMax, filter.prefFilterLengthMaxOn ));
        table_dialog.addView( new Entry<>(context, "Gain min (m)", filter.prefFilterGainMin, filter.prefFilterGainMinOn ));
        table_dialog.addView( new Entry<>(context, "Gain max (m)", filter.prefFilterGainMax, filter.prefFilterGainMaxOn ));


        {
            TableRow row = new TableRow(dialogContext);
            TableLayout.LayoutParams params = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0,30,10,0);
            row.setLayoutParams(params);
            row.setBackgroundColor(context.getColor(R.color.GRAY200));


            TextView tvSpace = createTextView(row, "", 10);
            tvSpace.setBackgroundColor(context.getColor(R.color.GRAY200));

            TextView tvCancel = createTextView(row, "CANCEL", 15);
            tvCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    trackStatisticActivity.prefFilterOn.toggle();
                    alertDialog.dismiss();
                }
            });
            tvCancel.setTypeface(null, Typeface.BOLD);

            TextView tvOK = createTextView(row, "OK", 15);
            tvOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i=0; i<table_dialog.getChildCount(); i++){
                        if (table_dialog.getChildAt(i) instanceof Entry<?>) {
                            Entry<?> entry = (Entry<?>) table_dialog.getChildAt(i);
                            entry.confirm();
                        }
                    }
                    trackStatisticActivity.prefFilterChanged.toggle();
                    alertDialog.dismiss();
                }
            });
            tvOK.setTypeface(null, Typeface.BOLD);

            table_dialog.addView(row);
        }

        builder.setView(table_dialog);

        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();



    }



    private TextView createTextView(ViewGroup parent, String text, float weight){
        TextView tv = new TextView(parent.getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(10,10,10,10);
        tv.setLayoutParams(params);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setPadding(5, 15, 5, 15);
        tv.setText(text);
        tv.setTextColor(parent.getContext().getColor(R.color.BLACK));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        parent.addView(tv);
        tv.setBackgroundColor(parent.getContext().getColor(R.color.GRAY100_A100));
        return tv;
    }


    private <T> EditPref<T> createEditPref(ViewGroup parent, Pref<T> pref, float weight){
        EditPref<T> et = new EditPref<>(parent.getContext(), pref);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(10,10,10,10);
        et.setLayoutParams(params);
        et.setGravity(Gravity.CENTER_HORIZONTAL);
        et.setPadding(5, 15, 5, 15);
//        et.setText( pref.getValue().toString() );
        et.setTextColor(parent.getContext().getColor(R.color.WHITE));
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        parent.addView(et);
        et.setBackgroundColor(parent.getContext().getColor(R.color.GRAY100_A100));
        return et;
    }

    private CheckBox createCheckBox(ViewGroup parent, Pref<Boolean> pref, float weight){
        CheckBox cb = new CheckBox(parent.getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        params.weight = weight;
        params.setMargins(10,10,10,10);
        cb.setLayoutParams(params);
        cb.setGravity(Gravity.CENTER_HORIZONTAL);
        cb.setPadding(5, 15, 5, 15);
        cb.setBackgroundColor(parent.getContext().getColor(R.color.GRAY100_A100));
        cb.setChecked(pref.getValue());
        parent.addView(cb);
        return cb;
    }

}
