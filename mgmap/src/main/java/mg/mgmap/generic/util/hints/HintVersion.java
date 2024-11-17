package mg.mgmap.generic.util.hints;

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

public class HintVersion extends AbstractHint{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static boolean shown = false;
    private final Pref<String> prefVersion;


    public HintVersion(MGMapActivity mgMapActivity){
        super(mgMapActivity, R.string.hintVersion);
        title = "MGMapViewer "+BuildConfig.VERSION_NAME.replaceFirst("-.*","");

        spanText = """
                New and noteworthy:
                - two line menu
                - file manager
                - geo intent
                - Android 14 support

                For details check also the changelog

                """;
        showAlways = true;

        prefVersion = mgMapActivity.getPrefCache().get(R.string.FSControl_pref_version_key, "");
    }

    public String getHeadline() {
        return title;
    }

    @Override
    public boolean noticeSpannableString(SpannableString spannableString) {
        String baseUrl = activity.getResources().getString(R.string.url_doc_main).replace("index.html","");
        createLink(spannableString, "two line menu", baseUrl+"Features/FurtherFeatures/QuickControl/qcs_mgmapactivity.html#two-line-menu");
        createLink(spannableString, "file manager", baseUrl+"Features/FurtherFeatures/FileManager/filemanager.html");
        createLink(spannableString, "geo intent", baseUrl+"Features/FurtherFeatures/Geocode/geocode.html#geo-intents");
        createLink(spannableString, "changelog", "https://github.com/mg4gh/MGMapViewer/blob/master/changelog.txt");
        shown = true;
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

    @Override
    public boolean checkHintCondition() {
        mgLog.d("currentVersion="+currentVersion()+" prefVersion="+prefVersion.getValue());
        return super.checkHintCondition() && !shown && (!currentVersion().equals(prefVersion.getValue()));
    }

    public static String currentVersion(){
        return BuildConfig.VERSION_NAME.replaceFirst("-.*","");
    }

}
