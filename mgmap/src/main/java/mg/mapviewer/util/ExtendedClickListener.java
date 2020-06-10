package mg.mapviewer.util;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import mg.mapviewer.MGMapApplication;

public class ExtendedClickListener implements View.OnClickListener {

    private static Handler timer = new Handler();
    private long doubleClickTimeout = 200;
    private class TTSingle implements Runnable{
        @Override
        public void run() {
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" single");
            ttSingle = null;
            onSingleClick(view);
        }
    };
    private TTSingle ttSingle = null;
    private View view = null;


    @Override
    public void onClick(View v) {
        view = v;
        if (ttSingle == null){
            ttSingle = new TTSingle();
            timer.postDelayed(ttSingle,doubleClickTimeout);
        } else {
            timer.removeCallbacks(ttSingle);
            ttSingle = null;
            onDoubleClick(view);
        }
    }

    public void onSingleClick(View view){}

    public void onDoubleClick(View view){
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" double");
        onSingleClick(view);
        onSingleClick(view);
    }
}
