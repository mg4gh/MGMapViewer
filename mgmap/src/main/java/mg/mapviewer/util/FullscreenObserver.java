package mg.mapviewer.util;

import android.app.Activity;
import android.view.View;

import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.R;

public class FullscreenObserver implements Observer {

    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.FSFullscreen_qc_On, true);

    private final Activity activity;

    public FullscreenObserver(Activity activity){
        this.activity = activity;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (prefFullscreen.getValue()){
            setFullscreen(activity);
        } else {
            hideFullscreen(activity);
        }
    }

    public void setFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
    public void hideFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

}
