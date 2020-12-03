package mg.mapviewer.util;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import org.mapsforge.map.model.DisplayModel;

import mg.mapviewer.MGMapApplication;

public class HintControl extends ExtendedClickListener {

    private final TextView tvHint;

    private Handler timer = new Handler();
    private Runnable ttHide = new Runnable() {
        @Override
        public void run() {
            tvHint.setVisibility(View.INVISIBLE);
        }
    };

    public HintControl(TextView tvHint){
        this.tvHint = tvHint;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        timer.removeCallbacks(ttHide); // cancel old timer if exists
        timer.postDelayed(ttHide, 1500);

        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            String text = tv.getText().toString();
            tvHint.setText(text);
            if (tv.getBackground() instanceof ColorDrawable) {
                ColorDrawable cd = (ColorDrawable) tv.getBackground();
                int colorCode = cd.getColor();
                tvHint.setBackgroundColor(colorCode);
            }
            Drawable[] ds = tv.getCompoundDrawables();
            if (ds[0] != null){
                Drawable clone = ds[0].getConstantState().newDrawable();
                float scale = DisplayModel.getDeviceScaleFactor();
                clone.setBounds(0,0, (int)(30*scale),(int)(30*scale));
                tvHint.setCompoundDrawables(clone,null,null,null);
            }
            int fgColor = tv.getTextColors().getDefaultColor();
            tvHint.setTextColor(fgColor);
            tvHint.setVisibility(View.VISIBLE);
        }
    }
}
