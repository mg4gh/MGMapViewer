package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;

public class HintShareReceived extends AbstractHint implements Runnable{

    public HintShareReceived(Activity activity, int num){
        super(activity, R.string.hintShareReceived);
        title = "Received shared files";
        spanText = "You have received " + num + "file"+((num==1)?"":"s")+" via android share. "+
                "Go to the folder where you want to store these files and press R.drawable.save{0xFFC0C0C0,80,80}." +
                "Then files will be stored there.";
    }

}
