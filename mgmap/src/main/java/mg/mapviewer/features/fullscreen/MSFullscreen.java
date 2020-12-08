package mg.mapviewer.features.fullscreen;

import android.app.Activity;
import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;

public class MSFullscreen extends MGMicroService {

    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.MSFullscreen_qc_On, true);

    public MSFullscreen(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    public PrefTextView initQuickControl(PrefTextView ptv, String info) {
        ptv.setPrefData(new MGPref[]{prefFullscreen},
                new int[]{R.drawable.fullscreen, R.drawable.fullscreen});
        return ptv;
    }

    @Override
    protected void start() {
        super.start();
        prefFullscreen.addObserver(refreshObserver);
        refreshObserver.onChange();
    }

    @Override
    protected void stop() {
        super.stop();
        prefFullscreen.deleteObserver(refreshObserver);
    }

    @Override
    public void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (prefFullscreen.getValue()){
                    setFullscreen(getActivity());
                } else {
                    hideFullscreen(getActivity());
                }

            }
        });
    }

    public static void setFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
    public static void hideFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

}
