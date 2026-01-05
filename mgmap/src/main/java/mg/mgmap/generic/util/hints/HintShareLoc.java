package mg.mgmap.generic.util.hints;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandles;

import mg.mgmap.BuildConfig;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

public class HintShareLoc extends AbstractHint{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public HintShareLoc(Activity activity){
        super(activity, R.string.hintVersion);
        title = "Share Location - Attention!";
        allowAbort = true;
        showAlways = true;

        spanText = """
                You are about to activate the feature "Share Location".\
                This feature enables that your location is given to other users of this app, \
                which means your position is given outside of this device. \
                Location data will be transferred encrypted and only the receiver is able to decrypt. \
                For more information see ShareLoc documentation. \
                With pressing "Got it" you agree to this location sharing.
                
                You can revert this agreement by switching this feature here off again.
                Check also the privacy information.

                """;

    }

    public String getHeadline() {
        return title;
    }

    @Override
    public boolean noticeSpannableString(SpannableString spannableString) {
        String baseUrl = activity.getResources().getString(R.string.url_doc_main).replace("index.html","");
        createLink(spannableString, "ShareLoc", baseUrl+"Features/FurtherFeatures/ShareLoc/shareloc.html");
        createLink(spannableString, "privacy", baseUrl+"privacy.html");
        return true;
    }

    private void createLink(SpannableString spannableString, String text, String href){
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( href ));
                activity.startActivity(browserIntent);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        spannableString .setSpan(clickableSpan, spanText.indexOf(text), spanText.indexOf(text)+text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

}
