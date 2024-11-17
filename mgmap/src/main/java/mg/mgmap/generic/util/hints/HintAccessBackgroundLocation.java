package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;

public class HintAccessBackgroundLocation extends AbstractHint implements Runnable{

    public HintAccessBackgroundLocation(Activity activity){
        super(activity, R.string.hintAccessBackgroundLocation);
        title = "Access background location";

        spanText = """
                In the next "Location permission" screen you will be asked, when you want to grant this permission. \
                Track recording needs permanent access to location data, otherwise there will be huge gaps in the recorded track.

                For proper function select option "Allow all the time." on the next screen.""";
        showAlways = true;
    }

    public String getHeadline() {
        return title;
    }
}
