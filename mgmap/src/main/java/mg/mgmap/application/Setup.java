package mg.mgmap.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Sftp;
import mg.mgmap.generic.util.WiFiUtil;
import mg.mgmap.generic.util.basic.NameUtil;

public class Setup {

    static private final Object semaphore = new Object();

    private static final String TEST_CONFIG = "config.properties";
    private static final String TEST_OFF = "off";
    private static final String TEST_DATA = "testData";
    private static final String TEST_GROUP = "testgroup.properties";
    private static final String TEST_FILES = "files.properties";
    private static final String TEST_RESULT = "result.properties";
    private static final String TEST_SETUP = "testSetup";
    private static final String TEST_TEMP = "temp";
    private static final String TEST_APP_DIR = "appDir";

    private static final String DEFAULT_APP_DIR = "MGMapViewer";

    private String sAppDir = "MGMapViewer";
    private String preferencesName;
    private SharedPreferences sharedPreferences;

    public void init(MGMapApplication application){
        preferencesName = application.getPackageName() + "_preferences";

        Log.d(MGMapApplication.LABEL, NameUtil.context()+"Setup start");
        File baseDir = application.getExternalFilesDir(null);
        File testSetup = new File(baseDir, TEST_SETUP);
        File testConfig = new File(testSetup, TEST_CONFIG);
        File testOff = new File(testSetup, TEST_OFF);

        if (testConfig.exists() && !testOff.exists()){
            new Thread(() -> {
                try {
                    Log.d(MGMapApplication.LABEL, NameUtil.context()+"preferencesName="+preferencesName);
                    Log.d(MGMapApplication.LABEL, NameUtil.context()+"Setup run");

                    new Sftp(testConfig) {

                        @Override
                        protected boolean checkPreconditions() {
                            Log.d(MGMapApplication.LABEL, NameUtil.context()+"checkPreconditions start");
                            return (WiFiUtil.checkWLAN(application, props.getProperty(PROP_WIFI)));
                        }

                        @SuppressWarnings("ResultOfMethodCallIgnored")
                        @Override
                        public void doCopy() throws IOException, SftpException {
                            channelSftp.lcd(baseDir.getAbsolutePath());
                            File testTemp = new File(testSetup, TEST_TEMP);
                            PersistenceManager.deleteRecursivly(testTemp);
                            testTemp.mkdir();

                            channelSftp.cd(TEST_DATA);
                            if (channelSftp.pwd().endsWith(TEST_DATA)){
                                @SuppressWarnings("unchecked") Vector<ChannelSftp.LsEntry> vLsEntries = channelSftp.ls(channelSftp.pwd());
                                for (ChannelSftp.LsEntry lsEntry : vLsEntries){
                                    if (lsEntry.getFilename().startsWith("testgroup") && lsEntry.getAttrs().isDir()){
                                        SftpATTRS aTestgroup = stat(lsEntry.getFilename()+"/"+TEST_GROUP);
                                        SftpATTRS aTestfiles = stat(lsEntry.getFilename()+"/"+TEST_FILES);
                                        SftpATTRS aTestResult = stat(lsEntry.getFilename()+"/"+TEST_RESULT);
                                        if ((aTestgroup != null) && (aTestfiles != null) && (aTestResult == null)){
                                            // ok, use this testgroup for setup
                                            channelSftp.get(lsEntry.getFilename()+"/"+TEST_GROUP, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_GROUP);
                                            channelSftp.get(lsEntry.getFilename()+"/"+TEST_FILES, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_FILES);

                                            Properties pTestgroup = new Properties();
                                            pTestgroup.load(new FileInputStream(new File(testTemp, TEST_GROUP)));
                                            Properties pFiles = new Properties();
                                            pFiles.load(new FileInputStream(new File(testTemp, TEST_FILES)));

                                            sAppDir = pTestgroup.getProperty("appDir", sAppDir);
                                            preferencesName = pTestgroup.getProperty("preferences", preferencesName);
                                            boolean cleanup = "true".equalsIgnoreCase(pTestgroup.getProperty("cleanup", ""));
                                            File appDir = new File(baseDir,sAppDir);
                                            if (cleanup && !DEFAULT_APP_DIR.equals(sAppDir)){ // never delete MGMapViewer
                                                PersistenceManager.deleteRecursivly(appDir);
                                                SharedPreferences p = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
                                                p.edit().clear().apply();
                                            }
                                            //TODO: remove
//                                            PreferenceManager.getDefaultSharedPreferences(application).edit().clear().apply();

                                            for (Object oFilename  : pFiles.keySet()){
                                                String lFilename = oFilename.toString();
                                                String rFilename = pFiles.getProperty(lFilename);
                                                File lParent = new File(baseDir, sAppDir+"/"+lFilename).getParentFile();
                                                Log.d(MGMapApplication.LABEL, NameUtil.context()+"copy from="+TEST_APP_DIR+"/"+rFilename+" to "+sAppDir+"/"+lFilename);
                                                assert lParent != null;
                                                lParent.mkdirs();
                                                channelSftp.get(TEST_APP_DIR+"/"+rFilename, sAppDir+"/"+lFilename);
                                            }

                                        }
                                    }

                                }
                            }

                        }

                    }.copy();

                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                } finally {
                    synchronized (semaphore){
                        semaphore.notifyAll();
                    }
                }
            }).start();
            synchronized (semaphore){
                try {
                    semaphore.wait(60*1000);
                } catch (InterruptedException e) {
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" interrupted!!");
                }
            }
        }
        sharedPreferences = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        Log.d(MGMapApplication.LABEL, NameUtil.context()+"Setup end");
    }


    public String getAppDirName(){
        return sAppDir;
    }
    public String getPreferencesName(){
        return preferencesName;
    }
    public SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }
}
