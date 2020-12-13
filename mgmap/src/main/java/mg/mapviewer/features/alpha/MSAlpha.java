package mg.mapviewer.features.alpha;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MSAlpha extends MGMicroService {

    private final MGPref<Boolean> prefAlpha = MGPref.get(R.string.Layers_qc_showAlphaSlider, false);
    private final MGPref<Boolean> prefAlpha2 = MGPref.get(R.string.Layers_qc_showAlphaSlider2, false);

    public MSAlpha(MGMapActivity activity){
        super(activity);
    }

    @Override
    public PrefTextView initQuickControl(PrefTextView ptv, String info){
        ptv.setPrefData(new MGPref[]{prefAlpha, prefAlpha2},
                new int[]{R.drawable.slider});
        setSliderVisibility();
        return ptv;
    }

    @Override
    protected void start() {
        super.start();
        prefAlpha.addObserver(refreshObserver);
        prefAlpha2.addObserver(refreshObserver);
        prefAlpha.setValue(false);
        prefAlpha2.setValue(false);
    }

    @Override
    protected void stop() {
        super.stop();
        prefAlpha.deleteObserver(refreshObserver);
        prefAlpha2.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        setSliderVisibility();
    }

    private void setSliderVisibility(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int visibility = prefAlpha.getValue()?VISIBLE:INVISIBLE;
                int visibility2 = prefAlpha2.getValue()?VISIBLE:INVISIBLE;
                if ((visibility == VISIBLE) && (visibility2 == VISIBLE)){
                    if (refreshObserver.last == prefAlpha2){
                        prefAlpha.setValue(false);
                    } else {
                        prefAlpha2.setValue(false);
                    }
                } else {
                    getActivity().findViewById(R.id.bars).setVisibility(visibility);
                    getActivity().findViewById(R.id.bars2).setVisibility(visibility2);
                }
            }
        });
    }

}
