package mg.mgmap.activity.mgmap.features.shareloc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mgmap.R;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;

public class PersonMeView extends LinearLayout implements Observer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final SimpleDateFormat sdf = new SimpleDateFormat(FSShareLoc.DATE_FORMAT, Locale.getDefault());

    Activity activity;
    SharePerson person = null;

    public PersonMeView(Context context) {
        super(context);
        init(context);
    }

    public PersonMeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PersonMeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PersonMeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        this.activity = (Activity) context;
        View itemView = activity.getLayoutInflater().inflate(R.layout.shareloc_person_other, this, true);

    }

    public void setPerson(SharePerson person){
        if (this.person != person ){
            if (this.person != null){
                this.person.deleteObserver(this);
            }
            this.person = person;
            if (this.person != null){
                this.person.addObserver(this);
            }
        }
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        try {
            if ((person.crt != null) && (!person.crt.isEmpty())){
                SharePerson p = CryptoUtils.getPersonData(new ByteArrayInputStream(person.crt.getBytes()));
                TextView tvCrt = this.findViewById(R.id.tvCertValidity);
                tvCrt.setText("Certificate valid until: "+sdf.format(p.shareWithUntil));
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.person != null) {
            this.person.deleteObserver(this);
        }
    }

}
