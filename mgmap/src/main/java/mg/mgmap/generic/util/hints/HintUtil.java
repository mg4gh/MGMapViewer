package mg.mgmap.generic.util.hints;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AlertDialogLayout;
import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;

public class HintUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final HashSet<Pref<Boolean>> prefs = new HashSet<>();

    CheckBox cbHideThis = null;
    CheckBox cbHideAll = null;

    private void registerHint(AbstractHint hint){
        mgLog.d("new: "+hint.prefShowHint);
        if (prefs.isEmpty()){ // first call
            hint.prefShowHints.addObserver((e) -> prefs.forEach(p -> p.setValue(true))); // add observer to cleanup, if changed
        }
        prefs.add(hint.prefShowHint);
        mgLog.d("all:"+prefs);
    }

    @SuppressLint({"DiscouragedApi", "SetTextI18n"})
    public boolean showHint(AbstractHint hint){
        if (hint == null) return false;
        try {
            registerHint(hint);
            Activity activity = hint.getActivity();
            String headline = hint.getHeadline();
            String hintText = hint.getText();

            if (hint.checkHintCondition()) {
                mgLog.d("showHint key="+hint.prefShowHint.getKey());
                LinearLayout ll = new LinearLayout(activity);
                ll.setLayoutParams(new AlertDialogLayout.LayoutParams(-2, -2));
                ll.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
                ll.setOrientation(LinearLayout.VERTICAL);

                TextView tv = new TextView(activity);
                tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                tv.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));

                SpannableString string = new SpannableString(hintText);
                int pos = 0;
                String separator = "R.drawable.";
                while ((pos = hintText.indexOf(separator, pos)) >= 0) {
                    int pos0 = pos + separator.length();
                    int pos1 = hintText.indexOf("{", pos);
                    int pos2 = hintText.indexOf("}", pos1);
                    String resName = hintText.substring(pos0, pos1);
                    String[] params = hintText.substring(pos1 + 1, pos2).split(",");

                    int drawableResourceId = activity.getResources().getIdentifier(resName, "drawable", activity.getPackageName());
                    Drawable myImage = ResourcesCompat.getDrawable(activity.getResources(), drawableResourceId, activity.getTheme());
                    ShapeDrawable background = new ShapeDrawable();

                    long color = Long.decode(params[0]);
                    background.getPaint().setColor((int) color);
                    LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, myImage});
                    layerDrawable.setBounds(0, 0, Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                    ImageSpan image = new ImageSpan(layerDrawable);
                    string.setSpan(image, pos, pos2 + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    pos = pos2;
                }
                if (hint.noticeSpannableString(string)){
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                }
                tv.setText(string);
                ll.addView(tv);

                if (!hint.showAlways){
                    cbHideThis = new CheckBox(activity);
                    cbHideThis.setChecked(!hint.prefShowHint.getValue() || hint.showOnce);
                    {
                        LinearLayout ll2 = new LinearLayout(activity);
                        ll2.setLayoutParams(new AlertDialogLayout.LayoutParams(-2, -2));
                        ll2.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
                        ll2.setOrientation(LinearLayout.HORIZONTAL);
                        ll.addView(ll2);

                        LinearLayout.LayoutParams paramsCB = new LinearLayout.LayoutParams(0, -2);
                        paramsCB.weight = 12;
                        paramsCB.gravity = Gravity.CENTER;
                        cbHideThis.setLayoutParams(paramsCB);
                        ll2.addView(cbHideThis);

                        TextView tv2 = new TextView(activity);
                        LinearLayout.LayoutParams paramsTV = new LinearLayout.LayoutParams(0, -2);
                        paramsTV.weight = 80;
                        tv2.setLayoutParams(paramsTV);
                        tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                        tv2.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
                        tv2.setText("Don't show this hint anymore.");
                        ll2.addView(tv2);
                    }

                    cbHideAll = new CheckBox(activity);
                    cbHideAll.setChecked(!hint.prefShowHints.getValue());
                    {
                        LinearLayout ll2 = new LinearLayout(activity);
                        ll2.setLayoutParams(new AlertDialogLayout.LayoutParams(-2, -2));
                        ll2.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
                        ll2.setOrientation(LinearLayout.HORIZONTAL);
                        ll.addView(ll2);

                        LinearLayout.LayoutParams paramsCB = new LinearLayout.LayoutParams(0, -2);
                        paramsCB.weight = 12;
                        paramsCB.gravity = Gravity.CENTER;
                        cbHideAll.setLayoutParams(paramsCB);
                        ll2.addView(cbHideAll);

                        TextView tv2 = new TextView(activity);
                        LinearLayout.LayoutParams paramsTV = new LinearLayout.LayoutParams(0, -2);
                        paramsTV.weight = 80;
                        tv2.setLayoutParams(paramsTV);
                        tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                        tv2.setPadding(ControlView.dp(5),ControlView.dp(5),ControlView.dp(5),ControlView.dp(5));
                        tv2.setText("Don't show hints anymore.");
                        ll2.addView(tv2);
                    }
                }

                DialogView dialogView = activity.findViewById(R.id.dialog_parent);
                dialogView.lock(() -> dialogView
                        .setTitle(headline)
                        .setContentView(ll)
                        .setLogPrefix(hint.getClass().getSimpleName())
                        .setPositive("Got it", evt -> {
                            if (cbHideThis != null){
                                hint.prefShowHint.setValue(!cbHideThis.isChecked());
                            }
                            if (cbHideAll != null){
                                hint.prefShowHints.setValue(!cbHideAll.isChecked());
                            }
                            hint.gotItActions.forEach(Runnable::run);
                        })
                        .setNegative( hint.isAllowAbort()?"Abort":null, null)
                        .show());
                return true;
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return false;
    }
}
