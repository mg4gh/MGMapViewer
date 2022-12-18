package mg.mgmap.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Sftp;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.WiFiUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;

public class Setup {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static private final Object semaphore = new Object();

    private static final String TEST_CONFIG = "config.properties";
    private static final String TEST_OFF = "off";
    private static final String TEST_DATA = "testData";
    private static final String TEST_GROUP = "testgroup.properties";
    private static final String TEST_FILES = "files.properties";
    private static final String TEST_PREFERENCES = "preferences.properties";
    private static final String TEST_RESULT = "result.properties";
    private static final String TEST_SETUP = "testSetup";
    private static final String TEST_TEMP = "temp";
    private static final String TEST_APP_DIR = "appDir";
    private static final String TEST_CASES = "tests.properties";

    private static final String DEFAULT_APP_DIR = "MGMapViewer";

    private MGMapApplication mgMapApplication;

    private String sAppDir = "MGMapViewer";
    private String preferencesName;
    private SharedPreferences sharedPreferences;
    private boolean testMode = false;
    private Handler timer = null;
    private final TreeMap<String, String> testCases = new TreeMap<>();
    private final Object token = new Object();
    private final Properties pTestResults = new Properties();
    String testgroup = null;

    File baseDir;
    File testSetup;
    File testConfig;

    public void init(MGMapApplication application){
        this.mgMapApplication = application;
        preferencesName = application.getPackageName() + "_preferences";

        mgLog.d("Setup start");
        baseDir = application.getExternalFilesDir(null);
        testSetup = new File(baseDir, TEST_SETUP);
        testConfig = new File(testSetup, TEST_CONFIG);
        File testOff = new File(testSetup, TEST_OFF);

        if (testConfig.exists() && !testOff.exists()){
            new Thread(() -> {
                try {
                    mgLog.d("preferencesName="+preferencesName);
                    mgLog.d("Setup run");

                    new Sftp(testConfig) {

                        @Override
                        protected boolean checkPreconditions() {
                            mgLog.d("checkPreconditions start");
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
                                        SftpATTRS aTestResult = stat(lsEntry.getFilename()+"/"+TEST_RESULT);
                                        if ((aTestgroup != null) && (aTestResult == null)){
                                            // ok, use this testgroup for setup
                                            testgroup = lsEntry.getFilename();
                                            testMode = true;
                                            channelSftp.get(lsEntry.getFilename()+"/"+TEST_GROUP, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_GROUP);
                                            Properties pTestgroup = new Properties();
                                            pTestgroup.load(new FileInputStream(new File(testTemp, TEST_GROUP)));

                                            sAppDir = pTestgroup.getProperty("appDir", sAppDir);
                                            preferencesName = pTestgroup.getProperty("preferences", preferencesName);
                                            boolean cleanup = "true".equalsIgnoreCase(pTestgroup.getProperty("cleanup", ""));
                                            sharedPreferences = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);

                                            File appDir = new File(baseDir,sAppDir);
                                            if (cleanup && !DEFAULT_APP_DIR.equals(sAppDir)){ // never delete MGMapViewer
                                                PersistenceManager.deleteRecursivly(appDir);
                                                sharedPreferences.edit().clear().apply();
                                            }

                                            SftpATTRS aTestfiles = stat(lsEntry.getFilename()+"/"+TEST_FILES);
                                            if (aTestfiles != null){
                                                channelSftp.get(lsEntry.getFilename()+"/"+TEST_FILES, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_FILES);
                                                Properties pFiles = new Properties();
                                                pFiles.load(new FileInputStream(new File(testTemp, TEST_FILES)));

                                                for (Object oFilename  : pFiles.keySet()){
                                                    String lFilename = oFilename.toString();
                                                    String rFilename = pFiles.getProperty(lFilename);
                                                    File lParent = new File(baseDir, sAppDir+"/"+lFilename).getParentFile();
                                                    mgLog.d("copy from="+TEST_APP_DIR+"/"+rFilename+" to "+sAppDir+"/"+lFilename);
                                                    assert lParent != null;
                                                    lParent.mkdirs();
                                                    channelSftp.get(TEST_APP_DIR+"/"+rFilename, sAppDir+"/"+lFilename);
                                                }
                                            }

                                            SftpATTRS aTestPreferences = stat(lsEntry.getFilename()+"/"+TEST_PREFERENCES);
                                            if (aTestPreferences != null){
                                                channelSftp.get(lsEntry.getFilename()+"/"+TEST_PREFERENCES, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_PREFERENCES);
                                                Properties pPreferences = new Properties();
                                                pPreferences.load(new FileInputStream(new File(testTemp, TEST_PREFERENCES)));

                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                for (Object oPrefName  : pPreferences.keySet()){
                                                    editor.putString( oPrefName.toString(), pPreferences.getProperty(oPrefName.toString()));
                                                }
                                                editor.apply();
                                            }

                                            SftpATTRS aTestCases = stat(lsEntry.getFilename()+"/"+TEST_CASES);
                                            if (aTestCases != null){
                                                channelSftp.get(lsEntry.getFilename()+"/"+TEST_CASES, TEST_SETUP+"/"+TEST_TEMP+"/"+TEST_CASES);
                                                Properties pPreferences = new Properties();
                                                pPreferences.load(new FileInputStream(new File(testTemp, TEST_CASES)));
                                                for (Object oPrefName  : pPreferences.keySet()) {
                                                    testCases.put(oPrefName.toString(), pPreferences.getProperty(oPrefName.toString()));
                                                }
                                            }

                                            application.registerActivityLifecycleCallbacks(application.getTestControl());
                                            break;
                                        }
                                    }

                                }
                            }

                        }

                    }.copy();

                } catch (Exception e) {
                    mgLog.e(e);
                } finally {
                    synchronized (semaphore){
                        semaphore.notifyAll();
                    }
                }
            }).start();
            WaitUtil.doWait(semaphore,60*1000);
        }
        sharedPreferences = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        application.getTestControl().setTestMode(isTestMode());
        mgLog.d("Setup end");

        if (isTestMode()){
            timer = new Handler();
            timer.postDelayed( testManager, 7000); // wait initial time before starting the tests
        }
    }

    Runnable testManager = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            new Thread(() -> {
                for (Map.Entry<String, String> tcEntry : testCases.entrySet()){
                    try {
                        Class<? extends  AbstractTestCase> testCaseClazz = (Class<? extends AbstractTestCase>) Class.forName(tcEntry.getValue());
                        Constructor<? extends  AbstractTestCase> constructor = testCaseClazz.getConstructor(MGMapApplication.class);
                        AbstractTestCase testCase = constructor.newInstance(mgMapApplication);

                        testCase.start();
                        new Thread(testCase::run).start();

                        long limit = System.currentTimeMillis() + testCase.getDurationLimit();
                        while (System.currentTimeMillis() < limit){
                            WaitUtil.doWait(token, 1000);
                            if (! testCase.isRunning()) break; // leave loop if testcase is finished
                        }
                        testCase.stop();
                        String result = testCase.getResult();
                        pTestResults.put(testCase.getName(), result);
                        mgLog.d(" finished - result: "+result );
                        WaitUtil.doWait(token, 1000);
                    } catch (Exception e) {
                        mgLog.e(e);
                    }
                }

                try {
                    pTestResults.store(new FileOutputStream(new File(testSetup, TEST_TEMP+"/"+TEST_RESULT)),"xxxyyyzzz");
                    new Sftp(testConfig) {
                        @Override
                        protected void doCopy() throws SftpException {
                            channelSftp.cd(TEST_DATA);
//                                channelSftp.put(testSetup.getAbsolutePath()+"/"+TEST_TEMP+"/"+TEST_RESULT, testgroup+"/"+TEST_RESULT);
                        }
                    }.copy();
                } catch (Exception e) {
                    mgLog.e(e);
                }

            }).start();

         }
    };


    public String getAppDirName(){
        return sAppDir;
    }
    public String getPreferencesName(){
        return preferencesName;
    }
    public SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }
    public boolean isTestMode() {
        return testMode;
    }
}
