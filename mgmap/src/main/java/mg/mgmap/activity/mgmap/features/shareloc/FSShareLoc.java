package mg.mgmap.activity.mgmap.features.shareloc;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSShareLoc extends FeatureService {

    Pref<Boolean> toggleShareLocation = new Pref<>(true);

    public FSShareLoc(MGMapActivity mmActivity) {
        super(mmActivity);
        toggleShareLocation.addObserver(plc -> {
            new LocationSettingsDialog(mmActivity).show();
        });
    }


    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("shareloc".equals(info)){
            etv.setData(R.drawable.shareloc);
//            etv.setData(prefRecordTrack, R.drawable.record_track1,R.drawable.record_track2);
            etv.setPrAction(toggleShareLocation);
            etv.setHelp(r(R.string.FSRecording_qcRec_help)).setHelp(r(R.string.FSRecording_qcRec_help1),r(R.string.FSRecording_qcRec_help2));
        }
        return etv;
    }

}
