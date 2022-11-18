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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

import mg.mgmap.BuildConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.SHA256;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Zipper;
import mg.mgmap.generic.util.hints.InitialMapDownload2;

public class DownloadPreferenceScreen extends MGPreferenceScreen {

    private static final String FTP_CONFIG_FILE = "ftp_config.properties";
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.download_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        InitialMapDownload2 hint = new InitialMapDownload2(getActivity());
        setBrowseIntent(R.string.preferences_dl_maps_wd_key, R.string.url_oam_dl, hint);
        setBrowseIntent(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu, hint);
        setBrowseIntent(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de, hint);

        setBrowseIntent(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);

        setBrowseIntent(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        setSWLatestOCL();
        setSWLocalOCL();

        Context context = requireContext().getApplicationContext();
        if (context instanceof MGMapApplication) {
            MGMapApplication application = (MGMapApplication) context;
            if (application.getPersistenceManager().getConfigProperties(null, FTP_CONFIG_FILE).size() == 0){
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
                    Log.e(MGMapApplication.LABEL, NameUtil.context()+" failed to add job");
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

//                    ArrayList<BgJob> jobs = new ArrayList<>();
                BgJobGroup bgJobGroup = new BgJobGroup(application, activity, Objects.requireNonNull(prefSwLocal.getTitle()).toString(), new BgJobGroupCallback(){} );
                BgJob job = new BgJob(){
                    @Override
                    protected void doJob() throws Exception {
                        Log.i(MGMapApplication.LABEL, NameUtil.context());
                        Properties props = application.getPersistenceManager().getConfigProperties(null, FTP_CONFIG_FILE);
                        String host = props.getProperty("FTP_SERVER");
                        int port = Integer.parseInt( props.getProperty("PORT"));
                        String username = props.getProperty("USERNAME");
                        String password = props.getProperty("PASSWORD");
                        String emulator = props.getProperty("EMULATOR");

                        PersistenceManager persistenceManager = application.getPersistenceManager();
                        persistenceManager.cleanApkDir();

                        FTPClient mFTPClient = new FTPClient();
                        // connecting to the host
                        mFTPClient.connect(host, port);
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" connect rc="+mFTPClient.getReplyCode());
                        // now check the reply code, if positive mean connection success
                        if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
                            // login using username & password
                            boolean status = mFTPClient.login(username, password);
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" status="+status);

                            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" filetype rc="+mFTPClient.getReplyCode());

                            if ((emulator != null) && ("true".endsWith(emulator))){
                                mFTPClient.enterLocalPassiveMode();
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" enterLocalPassiveMode rc="+mFTPClient.getReplyCode());
                            } else {
                                mFTPClient.enterLocalActiveMode();
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" enterLocalActiveMode rc="+mFTPClient.getReplyCode());
                            }
                            mFTPClient.setRemoteVerificationEnabled(false);
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" setRemoteVerificationEnabled rc="+mFTPClient.getReplyCode());

                            String remoteName = null;
                            for (String name : mFTPClient.listNames()){
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+ "name=\""+name+"\"");
                                if (name.endsWith(".apk")){
                                    remoteName = "/"+name;
                                }
                            }
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" remoteName="+remoteName);
                            boolean success = false;
                            if (remoteName != null){
                                {
                                    File localFile = new File(persistenceManager.getApkDir(), remoteName);
                                    OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(localFile));
                                    success = mFTPClient.retrieveFile(remoteName, outputStream1);
                                    outputStream1.close();
                                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+remoteName+" "+success);
                                }
                                {
                                    remoteName += ".sha256"; // try to copy also corresponding sha256 fingerprint
                                    File localFile = new File(persistenceManager.getApkDir(), remoteName);
                                    OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(localFile));
                                    success &= mFTPClient.retrieveFile(remoteName, outputStream1);
                                    outputStream1.close();
                                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+remoteName+" "+success);
                                }
                            }

                            mFTPClient.logout();
                            mFTPClient.disconnect();
                            if (success){
                                success = verifyAndInstall(application, persistenceManager);
                            }
                            if (success) {
                                bgJobGroup.setTitle(null);
                            } else {
                                throw new Exception("FTP Download not successful.");
                            }
                        } // if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
                    } // protected void doJob() throws Exception {
                };
                bgJobGroup.addJob(job);
                bgJobGroup.setConstructed(Objects.requireNonNull(prefSwLocal.getSummary()).toString());
            } else {
                Log.e(MGMapApplication.LABEL, NameUtil.context()+" expected MGMapApplication, found "+context.getClass().getName());
            }
            return true;
        });
    }

    private boolean verifyAndInstall(Context context, PersistenceManager persistenceManager){
        File file = persistenceManager.getApkFile();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Install file="+file.getAbsolutePath());
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Install size="+file.length());
        if (SHA256.verify(file)){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" checksum verification successful");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            return true;
        }
        return false;

    }

}
