package mg.mgmap.generic.util;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;

import java.util.Calendar;
import java.util.Objects;

import mg.mgmap.generic.util.basic.Formatter;

@SuppressLint("ViewConstructor")
public class EditPref<T> extends androidx.appcompat.widget.AppCompatEditText{

    final Pref<T> pref;

    public EditPref(Context context, Pref<T> pref) {
        super(context);
        this.pref = pref;

        addTextChangedListener(new TextWatcher() {
            private String old;
            private boolean calOk = true;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                old = charSequence.toString();
                if (pref.getValue() instanceof Calendar){
                    calOk = ((i2==10)); // call my only be changed by calendar picker
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (getText() != null) {
                    if ((!calOk) || (pref.verify(getText().toString()) == null)) {
                        if (pref.verify(old) != null){
                            setText(old);
                        }
                    }
                }
            }
        });

        setOnFocusChangeListener((view, focused) -> {
            if (focused){
                callOnClick();
            }
        });

        setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_SEARCH ) {
                KeyboardUtil.hideKeyboard(this);
            }
            return true;
        });
        setSingleLine();


        if (pref.getValue() instanceof Calendar) {
            @SuppressWarnings("unchecked")
            Pref<Calendar> prefC = (Pref<Calendar>)pref;
            setText( Formatter.SDF1a.format(prefC.getValue().getTime()) );

            setOnClickListener(v -> {
                Calendar c = prefC.verify( Objects.requireNonNull(getText()).toString() ); // get current value to calendar format
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        context,
                        (view, year, monthOfYear, dayOfMonth) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set( year, monthOfYear, dayOfMonth);
                            setText( Formatter.SDF1a.format(calendar.getTime()) );
                        },
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
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
