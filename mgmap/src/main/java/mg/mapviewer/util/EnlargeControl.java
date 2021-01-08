package mg.mapviewer.util;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import org.mapsforge.map.model.DisplayModel;

public class EnlargeControl extends ExtendedClickListener {

    private final TextView tvEnlarge;

    private final Handler timer = new Handler();
    private final Runnable ttHide = new Runnable() {
        @Override
        public void run() {
            tvEnlarge.setVisibility(View.INVISIBLE);
        }
    };

    public EnlargeControl(TextView tvEnlarge){
        this.tvEnlarge = tvEnlarge;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        timer.removeCallbacks(ttHide); // cancel old timer if exists
        timer.postDelayed(ttHide, 1500);

        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            String text = tv.getText().toString();
            tvEnlarge.setText(text);
            if (tv.getBackground() instanceof ColorDrawable) {
                ColorDrawable cd = (ColorDrawable) tv.getBackground();
                int colorCode = cd.getColor();
                tvEnlarge.setBackgroundColor(colorCode);
            }
            Drawable[] ds = tv.getCompoundDrawables();
            if (ds[0] != null){
                Drawable clone = ds[0].getConstantState().newDrawable();
                float scale = DisplayModel.getDeviceScaleFactor();
                clone.setBounds(0,0, (int)(30*scale),(int)(30*scale));
                tvEnlarge.setCompoundDrawables(clone,null,null,null);
            }
            int fgColor = tv.getTextColors().getDefaultColor();
            tvEnlarge.setTextColor(fgColor);
            tvEnlarge.setVisibility(View.VISIBLE);
        }
    }
}
