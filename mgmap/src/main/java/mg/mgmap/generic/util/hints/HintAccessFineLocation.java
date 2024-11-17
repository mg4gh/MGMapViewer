package mg.mgmap.generic.util.hints;

import android.app.Activity;


import mg.mgmap.R;

public class HintAccessFineLocation extends AbstractHint implements Runnable{

    public HintAccessFineLocation(Activity activity){
        super(activity, R.string.hintAccessFineLocation);
        title = "Access fine location";
        spanText = """
                You are about to start the GPS based location service. For this purpose you need to grant the access to the device's location.\
                Location data will be stored locally in the recorded track files and will NOT be shared unless you explicitly copy/share them.

                For proper function select option "While using the app" on the next screen.""";
        showAlways = true;
    }

    @Override
    public String getHeadline() {
        return title;
    }
}
