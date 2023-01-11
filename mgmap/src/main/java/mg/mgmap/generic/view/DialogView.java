package mg.mgmap.generic.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("unused")
public class DialogView extends RelativeLayout {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private String title = null;
    private View contentView = null;
    private ExtendedTextView neutralETV = null;
    private ExtendedTextView positiveETV = null;
    private ExtendedTextView negativeETV = null;
    private String logPrefix = "";
    private final int defaultPadding = ControlView.dp(10);

    private final Observer dismissObserver = evt -> reset();


    public DialogView(Context context) {
        super(context);
    }

    public DialogView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DialogView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    private void reset(){
        removeAllViews();
        setBackgroundColor(0x00FFFFFF);
        setClickable(false);
        title = null;
        contentView = null;
        neutralETV = null;
        positiveETV = null;
        negativeETV = null;
        logPrefix = "";
    }

    public String getTitle() {
        return title;
    }
    public DialogView setTitle(String title) {
        this.title = title;
        return this;
    }
    private View createTitleView(){
        AppCompatTextView tv = new AppCompatTextView(getContext());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setText(title);
        tv.setPadding(defaultPadding,defaultPadding,defaultPadding,defaultPadding);
        tv.setGravity(Gravity.START);
        return tv;
    }

    public DialogView setContentView(View contentView) {
        this.contentView = contentView;
        return this;
    }
    public DialogView setContent(String text) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        tv.setPadding(defaultPadding,defaultPadding,defaultPadding,defaultPadding);
        tv.setText(text);
        this.contentView = tv;
        return this;
    }

    public DialogView setPositive(String buttonText, Observer action){
        if (buttonText != null) {
            positiveETV = createButton(buttonText, logPrefix+"btPositive", action);
            positiveETV.addActionObserver(dismissObserver);
        }
        return this;
    }
    public DialogView setNegative(String buttonText, Observer action){
        if (buttonText != null){
            negativeETV = createButton(buttonText, logPrefix+"btNegative", action);
            negativeETV.addActionObserver(dismissObserver);
        }
        return this;
    }
    public DialogView setNeutral(String buttonText, Observer action){
        if (buttonText != null) {
            neutralETV = createButton(buttonText, logPrefix+"btNeutral", action);
        }
        return this;
    }

    public DialogView setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix.replaceAll(" ","_")+"_";
        return this;
    }

    protected ExtendedTextView createButton(String text, String logName, Observer observer){
        ExtendedTextView etv = new ExtendedTextView(getContext());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
        etv.setLayoutParams(buttonParams);
        etv.setPadding(defaultPadding*2,defaultPadding,defaultPadding*2,defaultPadding);
        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        etv.setTypeface(null, Typeface.BOLD);
        etv.setText(text);


        etv.setBackground(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.shape2, getContext().getTheme()));
        etv.setPrAction(new Pref<>(Boolean.FALSE));
        if (observer != null){
            etv.addActionObserver(observer);
        }
        etv.setName(logName);
        return etv;
    }

    public DialogView show(){
        LinearLayout llDialog = new LinearLayout(getContext());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(ControlView.dp(20),ControlView.dp(20),ControlView.dp(20),ControlView.dp(20));
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        llDialog.setLayoutParams(params);

        llDialog.setOrientation(LinearLayout.VERTICAL);
        llDialog.setPadding(defaultPadding,defaultPadding,defaultPadding,defaultPadding);
        llDialog.setBackgroundColor(0xFFFFFFFF);

        llDialog.addView(createTitleView());
        llDialog.addView(contentView);

        LinearLayout buttonView = new LinearLayout(getContext());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0,ControlView.dp(10), 0, 0);
        buttonView.setLayoutParams(buttonParams);

        if (neutralETV != null){
            buttonView.addView(neutralETV);
            neutralETV.setGravity(Gravity.CENTER);
        }
        Space spacer = new Space(getContext());
        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        spParams.weight = 1;
        spacer.setLayoutParams(spParams);
        buttonView.addView(spacer);

        if (negativeETV != null){
            buttonView.addView(negativeETV);
            negativeETV.setGravity(Gravity.CENTER);
        }

        if (positiveETV != null){
            buttonView.addView(positiveETV);
            positiveETV.setGravity(Gravity.CENTER);
        }
        llDialog.addView(buttonView);
        this.addView(llDialog);
        this.setBackgroundColor(0x30000000);
        this.setClickable(true);
        return this;
    }

}
