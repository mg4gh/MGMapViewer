package mg.mgmap.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import mg.mgmap.BuildConfig;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PersistenceManager;
import mg.mgmap.util.Zipper;

public class DownloadPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.download_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        setBrowseIntent(R.string.preferences_dl_maps_wd_key, R.string.url_oam_dl);
        setBrowseIntent(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu);
        setBrowseIntent(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de);

        setBrowseIntent(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);

        setBrowseIntent(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        setSWLatestOCL();
    }


    private void setSWLatestOCL(){
        Preference prefSwLatest = findPreference( getResources().getString(R.string.preferences_dl_sw_latest_key) );
        prefSwLatest.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                ArrayList<BgJob> jobs = new ArrayList(){ };
                Context context = getContext().getApplicationContext();
                BgJob job = new BgJob() {
                    @Override
                    protected void doJob() throws Exception {
                        super.doJob();
                        Zipper zipper = new Zipper(null);
                        String urlString = getResources().getString(R.string.url_github_apk_latest)+((BuildConfig.DEBUG)?"debug":"release")+"/apk.zip";
                        URL url = new URL(urlString);
                        PersistenceManager.getInstance().cleanApkDir();
                        zipper.unpack(url, PersistenceManager.getInstance().getApkDir(), null, this);

                        File file = PersistenceManager.getInstance().getApkFile();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

                        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                };
                jobs.add(job);

                if (context instanceof MGMapApplication) {
                    MGMapApplication application = (MGMapApplication) context;
                    application.addBgJobs(jobs);
                } else {
                    Log.e(MGMapApplication.LABEL, NameUtil.context()+" failed to add job");
                }

                return true;
            }
        });
    }

}
