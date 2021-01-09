package mg.mgmap.settings;

import android.content.Intent;
import android.net.Uri;

import androidx.preference.PreferenceFragmentCompat;

public abstract class MGPreferenceScreen extends PreferenceFragmentCompat {

    protected void setIntent(int resId, Intent intent){
        findPreference( getResources().getString(resId) ).setIntent( intent );
    }

    protected void setBrowseIntent(int resId, int uriId){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uriId)));
        setIntent(resId, browserIntent);
    }



}
