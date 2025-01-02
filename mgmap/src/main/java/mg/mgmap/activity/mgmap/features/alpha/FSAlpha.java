/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.alpha;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class FSAlpha extends FeatureService {

    private final Pref<Boolean> prefAlphaLayers = getPref(R.string.FSAlpha_qc_showAlphaLayers, false);
    private final Pref<Boolean> prefAlphaTracks = getPref(R.string.FSAlpha_qc_showAlphaTracks, false);

    private final Pref<Boolean> prefStlVisibility = getPref(R.string.FSATL_pref_STL_visibility, false);
    private final Pref<Boolean> prefAtlVisibility = getPref(R.string.FSATL_pref_ATL_visibility, false);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Boolean> prefRtlVisibility = getPref(R.string.FSRecording_pref_RTL_visibility, false);
    private final Pref<Boolean> prefSliderTracksEnabled = new Pref<>(false);

    public FSAlpha(MGMapActivity activity){
        super(activity);

        Observer prefSliderTracksObserver =
                (e)-> prefSliderTracksEnabled.setValue( prefStlVisibility.getValue() || prefAtlVisibility.getValue() || prefMtlVisibility.getValue() || prefRtlVisibility.getValue() );
        prefStlVisibility.addObserver(prefSliderTracksObserver);
        prefAtlVisibility.addObserver(prefSliderTracksObserver);
        prefMtlVisibility.addObserver(prefSliderTracksObserver);
        prefRtlVisibility.addObserver(prefSliderTracksObserver);
        prefSliderTracksObserver.propertyChange(null);

        prefAlphaLayers.addObserver(refreshObserver);
        prefAlphaTracks.addObserver(refreshObserver);
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("alpha_layers".equals(info)){
            etv.setPrAction(prefAlphaLayers);
            etv.setData(prefAlphaLayers,R.drawable.slider_layer2,R.drawable.slider_layer1);
            etv.setHelp(r(R.string.FSAlpha_qcAlphaLayers_Help)).setHelp(r(R.string.FSAlpha_qcAlphaLayers_Help1),r(R.string.FSAlpha_qcAlphaLayers_Help2));
        } else if ("alpha_tracks".equals(info)){
            etv.setPrAction(prefAlphaTracks);
            etv.setData(prefAlphaTracks,R.drawable.slider_track2,R.drawable.slider_track1);
            etv.setDisabledData(prefSliderTracksEnabled, R.drawable.slider_track_dis);
            etv.setHelp(r(R.string.FSAlpha_qcAlphaTracks_Help)).setHelp(r(R.string.FSAlpha_qcAlphaTracks_Help1),r(R.string.FSAlpha_qcAlphaTracks_Help2));
        }
        setSliderVisibility();
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAlphaLayers.setValue(false);
        prefAlphaTracks.setValue(false);
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        setSliderVisibility();
    }

    private void setSliderVisibility(){
        int visibility = prefAlphaLayers.getValue()?VISIBLE:INVISIBLE;
        int visibility2 = prefAlphaTracks.getValue()?VISIBLE:INVISIBLE;
        if ((visibility == VISIBLE) && (visibility2 == VISIBLE)){
            if (refreshObserver.last == prefAlphaTracks){
                prefAlphaLayers.setValue(false);
            } else {
                prefAlphaTracks.setValue(false);
            }
        } else {
            getActivity().findViewById(R.id.bars).setVisibility(visibility);
            getActivity().findViewById(R.id.bars2).setVisibility(visibility2);
        }
        getControlView().reworkLabeledSliderVisibility();
        getControlView().reworkLabeledSliderVisibility2();
    }
}
