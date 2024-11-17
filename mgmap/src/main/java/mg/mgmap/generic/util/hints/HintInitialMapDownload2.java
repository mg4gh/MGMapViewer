package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;

public class HintInitialMapDownload2 extends AbstractHint implements Runnable{

    public HintInitialMapDownload2(Activity activity){
        super(activity, R.string.hintInitialMapDownload2);
        showOnce = false;
        allowAbort = true;
        title = "Map download";
        spanText = """
                Now your browser will open www.openandromaps.de map download page. \
                Find the map you want to download (e.g. R.drawable.germany{0xFFC0C0C0,300,80}). Press the R.drawable.plus{0xFFC0C0C0,80,80} \
                in front of the map name. Among the visible options select the line starting with
                "Android mf-V5-map:" and press R.drawable.im{0xFFC0C0C0,320,100}.""";
    }

}
