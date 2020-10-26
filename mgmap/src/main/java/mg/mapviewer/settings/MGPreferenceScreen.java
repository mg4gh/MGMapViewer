package mg.mapviewer.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.R;

public abstract class MGPreferenceScreen extends PreferenceFragmentCompat {

    protected void setIntent(int resId, Intent intent){
        findPreference( getResources().getString(resId) ).setIntent( intent );
    }

    protected void setBrowseIntent(int resId, int uriId){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uriId)));
        setIntent(resId, browserIntent);
    }



}
