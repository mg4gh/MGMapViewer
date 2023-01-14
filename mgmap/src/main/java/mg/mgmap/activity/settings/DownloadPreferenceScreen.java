/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Objects;
import java.util.Vector;

import mg.mgmap.BuildConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.SHA256;
import mg.mgmap.generic.util.Sftp;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Zipper;
import mg.mgmap.generic.util.hints.HintInitialMapDownload2;

public class DownloadPreferenceScreen extends MGPreferenceScreen {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final String LOCAL_APK_SYNC_PROPERTIES = "apk_sync.properties";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(requireContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.download_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        setBrowseIntent(R.string.preferences_dl_maps_wd_key, R.string.url_oam_dl, new HintInitialMapDownload2(getActivity()));
        setBrowseIntent(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu, new HintInitialMapDownload2(getActivity()));
        setBrowseIntent(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de, new HintInitialMapDownload2(getActivity()));

        setBrowseIntent(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);

        setBrowseIntent(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        setSWLatestOCL();
        setSWLocalOCL();

        Context context = requireContext().getApplicationContext();
        if (context instanceof MGMapApplication) {
            MGMapApplication application = (MGMapApplication) context;
            if (application.getPersistenceManager().getConfigProperties(null, LOCAL_APK_SYNC_PROPERTIES).size() == 0){
                Preference prefSwLocal = findPreference( getResources().getString(R.string.preferences_dl_sw_local_key) );
                assert prefSwLocal != null;
                prefSwLocal.setVisible(false);
            }
        }
    }


    private void setSWLatestOCL(){
        Preference prefSwLatest = findPreference( getResources().getString(R.string.preferences_dl_sw_latest_key) );
        assert prefSwLatest != null;
        prefSwLatest.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                Context context = requireContext().getApplicationContext();
                if (context instanceof MGMapApplication) {
                    MGMapApplication application = (MGMapApplication) context;
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    BgJobGroup bgJobGroup = new BgJobGroup(application, activity, Objects.requireNonNull(prefSwLatest.getTitle()).toString(), new BgJobGroupCallback(){} );
                    BgJob job = new BgJob() {
                        @Override
                        protected void doJob() throws Exception {
                            Zipper zipper = new Zipper(null);
                            String urlString = getResources().getString(R.string.url_github_apk_latest)+((BuildConfig.DEBUG)?"debug":"release")+"/apk.zip";
                            URL url = new URL(urlString);
                            PersistenceManager persistenceManager = application.getPersistenceManager();
                            persistenceManager.cleanApkDir();
                            zipper.unpack(url, persistenceManager.getApkDir(), null, this);
                            if (verifyAndInstall(context, persistenceManager)) {
                                bgJobGroup.setTitle(null);
                            } else {
                                throw new Exception("APK Download not successful.");
                            }
                        }
                    };
                    bgJobGroup.addJob(job);
                    bgJobGroup.setConstructed(Objects.requireNonNull(prefSwLatest.getSummary()).toString());
                } else {
                    mgLog.e("failed to add job");
                }
                return true;
            }
        });
    }

    private void setSWLocalOCL(){
        Preference prefSwLocal = findPreference( getResources().getString(R.string.preferences_dl_sw_local_key) );
        assert prefSwLocal != null;
        prefSwLocal.setOnPreferenceClickListener(preference -> {
            Context context = requireContext().getApplicationContext();
            if (context instanceof MGMapApplication) {
                MGMapApplication application = (MGMapApplication) context;
                SettingsActivity activity = (SettingsActivity) requireActivity();

                BgJobGroup bgJobGroup = new BgJobGroup(application, activity, Objects.requireNonNull(prefSwLocal.getTitle()).toString(), new BgJobGroupCallback(){} );
                BgJob job = new BgJob(){
                    @Override
                    protected void doJob() throws Exception {
                        mgLog.i();
                        PersistenceManager persistenceManager = application.getPersistenceManager();
                        persistenceManager.cleanApkDir();

                        new Sftp(new File(persistenceManager.getConfigDir(),LOCAL_APK_SYNC_PROPERTIES)){
                            @SuppressWarnings("unchecked")
                            @Override
                            protected void doCopy() throws SftpException {
                                channelSftp.lcd(persistenceManager.getApkDir().getAbsolutePath());
                                Vector<ChannelSftp.LsEntry> vLsEntries = channelSftp.ls(channelSftp.pwd());
                                for (ChannelSftp.LsEntry lsEntry : vLsEntries){
                                    if (!lsEntry.getAttrs().isDir() && lsEntry.getFilename().matches(".*\\.apk(\\.sha256)?")){
                                        mgLog.i("handle entry: "+lsEntry.getFilename());
                                        channelSftp.get(lsEntry.getFilename(),lsEntry.getFilename());
                                    }
                                }

                            }
                        }.copy();
                        if (verifyAndInstall(application, persistenceManager)) {
                            bgJobGroup.setTitle(null); // prevents jobGroup finished alert window
                        } else {
                            throw new Exception("SFTP Download not successful.");
                        }
                    }
                };
                bgJobGroup.addJob(job);
                bgJobGroup.setConstructed(Objects.requireNonNull(prefSwLocal.getSummary()).toString());
            } else {
                mgLog.e("expected MGMapApplication, found "+context.getClass().getName());
            }
            return true;
        });
    }

    private boolean verifyAndInstall(Context context, PersistenceManager persistenceManager){
        File file = persistenceManager.getApkFile();
        if (file != null){
            mgLog.i("Install file="+file.getAbsolutePath());
            mgLog.i("Install size="+file.length());
            if (SHA256.verify(file)){
                mgLog.i("checksum verification successful");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                return true;
            }
        } else {
            mgLog.e("apk file not found!");
        }
        return false;

    }

}
