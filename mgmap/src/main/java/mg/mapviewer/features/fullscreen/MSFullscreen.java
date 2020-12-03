package mg.mapviewer.features.fullscreen;

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
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (prefFullscreen.getValue()){
                    setFullscreen();
                } else {
                    hideFullscreen();
                }

            }
        });
    }

    void setFullscreen() {
        int newUiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
    void hideFullscreen() {
        int newUiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

}
