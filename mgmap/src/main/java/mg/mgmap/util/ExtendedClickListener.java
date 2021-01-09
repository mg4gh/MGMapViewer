package mg.mgmap.util;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import mg.mgmap.MGMapApplication;

public class ExtendedClickListener implements View.OnClickListener {

    private static Handler timer = new Handler();
    protected long doubleClickTimeout = 200;
    private class TTSingle implements Runnable{
        @Override
        public void run() {
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
        Log.v(MGMapApplication.LABEL, NameUtil.context()+" double");
        onSingleClick(view);
        onSingleClick(view);
    }
}
