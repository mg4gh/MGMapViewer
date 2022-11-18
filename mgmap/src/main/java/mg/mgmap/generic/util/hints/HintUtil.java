package mg.mgmap.generic.util.hints;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AlertDialogLayout;
import androidx.core.content.res.ResourcesCompat;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.NameUtil;

public class HintUtil {


    public static boolean showHint(AbstractHint hint){
        if (hint == null) return false;
        try {
            Activity activity = hint.getActivity();
            String headline = hint.getHeadline();
            String hintText = hint.getText();

            Pref<Boolean> prefShowHints = PrefCache.getApplicationPrefCache(activity).get(activity.getResources().getString(R.string.preferences_hints_key), true);
            if (prefShowHints.getValue() && hint.checkHintCondition()) {

                LinearLayout ll = new LinearLayout(activity);
                ll.setLayoutParams(new AlertDialogLayout.LayoutParams(-2, -2));
                ll.setPadding(20, 20, 20, 20);
                ll.setOrientation(LinearLayout.VERTICAL);

                TextView tv = new TextView(activity);
                tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                tv.setPadding(20, 20, 20, 20);

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
                tv.setText(string);
                ll.addView(tv);

                LinearLayout ll2 = new LinearLayout(activity);
                ll2.setLayoutParams(new AlertDialogLayout.LayoutParams(-2, -2));
                ll2.setPadding(20, 20, 20, 20);
                ll2.setOrientation(LinearLayout.HORIZONTAL);
                ll.addView(ll2);

                CheckBox cb = new CheckBox(activity);
                LinearLayout.LayoutParams paramsCB = new LinearLayout.LayoutParams(0, -2);
                paramsCB.weight = 10;
                paramsCB.gravity = Gravity.CENTER;
                cb.setLayoutParams(paramsCB);
                ll2.addView(cb);

                TextView tv2 = new TextView(activity);
                LinearLayout.LayoutParams paramsTV = new LinearLayout.LayoutParams(0, -2);
                paramsTV.weight = 80;
                tv2.setLayoutParams(paramsTV);
                tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                tv2.setPadding(20, 20, 20, 20);
                tv2.setText("I don't want to get hints anymore.");
                ll2.addView(tv2);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setTitle(headline)
                        .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int sumthin) {
                                if (cb.isChecked()){
                                    prefShowHints.setValue(false);
                                }
                                hint.gotItActions.forEach(Runnable::run);
                                dialog.dismiss();
                            }
                        })
                        .setView(ll);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getWindow().setLayout(-2, -2);
                return true;
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return false;
    }
}
