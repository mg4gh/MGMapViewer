package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;

public class HintMoveReceived extends AbstractHint implements Runnable{

    public HintMoveReceived(Activity activity, int num){
        super(activity, R.string.hintMoveReceived);
        title = "Move files";
        spanText = "You move " + num + " item"+((num==1)?"":"s")+". "+
                "Go to the target folder of this move operation and press R.drawable.save{0xFFC0C0C0,80,80}.";
    }

}
