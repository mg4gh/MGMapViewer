package mg.mapviewer.features.alpha;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;
import mg.mapviewer.view.PrefTextView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MSAlpha extends MGMicroService {

    private final MGPref<Boolean> prefAlpha = MGPref.get(R.string.Layers_qc_showAlphaSlider, false);
    private final MGPref<Boolean> prefAlpha2 = MGPref.get(R.string.Layers_qc_showAlphaSlider2, false);

    private final MGPref<Boolean> prefStlVisibility = MGPref.get(R.string.MSATL_pref_STL_visibility, false);
    private final MGPref<Boolean> prefAtlVisibility = MGPref.get(R.string.MSATL_pref_ATL_visibility, false);
    private final MGPref<Boolean> prefMtlVisibility = MGPref.get(R.string.MSMarker_pref_MTL_visibility, false);
    private final MGPref<Boolean> prefRtlVisibility = MGPref.get(R.string.MSRecording_pref_RTL_visibility, false);
    private final MGPref<Boolean> prefSliderTracksEnabled = MGPref.anonymous(false);

    public MSAlpha(MGMapActivity activity){
        super(activity);

        Observer  prefSliderTracksObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                prefSliderTracksEnabled.setValue( prefStlVisibility.getValue() || prefAtlVisibility.getValue() || prefMtlVisibility.getValue() || prefRtlVisibility.getValue() );
            }
        };
        prefStlVisibility.addObserver(prefSliderTracksObserver);
        prefAtlVisibility.addObserver(prefSliderTracksObserver);
        prefMtlVisibility.addObserver(prefSliderTracksObserver);
        prefRtlVisibility.addObserver(prefSliderTracksObserver);
        prefSliderTracksObserver.update(null, null);
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        if ("alpha_layers".equals(info)){
            etv.setPrAction(prefAlpha);
            etv.setData(R.drawable.slider_layer);
            etv.setHelp(r(R.string.MSAlpha_qcAlphaLayers_Help));
        } else if ("alpha_tracks".equals(info)){
            etv.setPrAction(prefAlpha2);
            etv.setData(R.drawable.slider_track);
            etv.setDisabledData(prefSliderTracksEnabled, R.drawable.slider_track_dis);
            etv.setHelp(r(R.string.MSAlpha_qcAlphaTracks_Help));
        }
        setSliderVisibility();
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAlpha.addObserver(refreshObserver);
        prefAlpha2.addObserver(refreshObserver);
        prefAlpha.setValue(false);
        prefAlpha2.setValue(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
                getControlView().reworkLabeledSliderVisibility();
            }
        });
    }

}
