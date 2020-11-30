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

    public MSAlpha(MGMapActivity activity){
        super(activity);
    }

    @Override
    public void initQuickControl(PrefTextView ptv, String info){
        ptv.setPrefData(new MGPref[]{prefAlpha},
                new int[]{},
                new int[]{R.drawable.slider});
        setSliderVisibility();
    }

    @Override
    protected void start() {
        super.start();
        prefAlpha.addObserver(refreshObserver);
        prefAlpha.setValue(false);
    }

    @Override
    protected void stop() {
        super.stop();
        prefAlpha.deleteObserver(refreshObserver);
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
                getActivity().findViewById(R.id.bars).setVisibility(visibility);
            }
        });
    }

}
