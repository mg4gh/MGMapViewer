package mg.mgmap.generic.util;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;

@SuppressLint("ViewConstructor")
public class EditPref<T> extends androidx.appcompat.widget.AppCompatEditText{

    Pref<T> pref;

    public EditPref(Context context, Pref<T> pref) {
        super(context);
        this.pref = pref;

        addTextChangedListener(new TextWatcher() {
            private String old;
            private boolean calOk = true;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                System.out.println("bef "+charSequence+" "+i+" "+i1+" "+i2);
                old = charSequence.toString();
                if (pref.getValue() instanceof Calendar){
                    calOk = ((i2==10)); // cal my only be changed by calendar picker
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                System.out.println("chg "+charSequence+" "+i+" "+i1+" "+i2);
            }
            @Override
            public void afterTextChanged(Editable editable) {
//                System.out.println("aft "+editable.toString());
                if (getText() != null) {
                    if ((!calOk) || (pref.verify(getText().toString()) == null)) {
                        setText(old);
                    }
                }
            }
        });

        if (pref.getValue() instanceof Calendar) {
            Pref<Calendar> prefC = (Pref<Calendar>)pref;
            setText( pref.sdf.format(prefC.getValue().getTime()) );

            setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean focused) {
                    if (focused){
                        callOnClick();
                    }
                }
            });
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Calendar c = prefC.verify( getText().toString() ); // get current value to calendar format
                    DatePickerDialog datePickerDialog = new DatePickerDialog(
                            context,
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year,
                                                      int monthOfYear, int dayOfMonth) {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.set( year, monthOfYear, dayOfMonth);
                                    setText( pref.sdf.format(calendar.getTime()) );
                                }
                            },
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    datePickerDialog.show();
                }
            });
        } else {
            setText( pref.getValue().toString() );
        }
    }

    public void confirm(){
        if (getText() != null){
            pref.setStringValue( getText().toString() );
        }
    }
}
