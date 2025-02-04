package mg.mgmap.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Properties;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class Setup {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final String WANTED_DEFAULT = "DEFAULT_SETUP";
    public static final String APP_DIR_DEFAULT = "MGMapViewer";
    private final MGMapApplication application;


    Setup(MGMapApplication application) {
        this.application = application;
    }

    public synchronized void wantSetup(String wanted, AssetManager assetManager)  {
        mgLog.d("wanted="+wanted);
        assert (wanted != null);

        try {
            application.cleanup();
            mgLog.d("Setup start: wanted="+wanted);
            String sAppDir = APP_DIR_DEFAULT;
            String preferencesName = application.getPackageName() + "_preferences";
            SharedPreferences sharedPreferences;
            BaseConfig.Mode mode = BaseConfig.Mode.NORMAL;

            File baseDir = application.getExternalFilesDir(null);
            if (wanted.equals(WANTED_DEFAULT)){
                sharedPreferences = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
            } else {
                // this code will only be used in instrumentation tests
                InputStream isTestgroup = assetManager.open(wanted+"/testgroup.properties");
                Properties pTestgroup = new Properties();
                pTestgroup.load(isTestgroup);

                sAppDir = pTestgroup.getProperty("appDir", sAppDir);
                PersistenceManager.deleteRecursivly(new File(baseDir, sAppDir)); // cleanup filesystem, if not yet empty
                preferencesName = pTestgroup.getProperty("preferences", preferencesName);
                sharedPreferences = application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
                sharedPreferences.edit().clear().apply(); // cleanup preferences, if not yet empty

                Properties pFiles = new Properties();
                pFiles.load(assetManager.open(wanted+"/files.properties"));
                for (Object oFilename  : pFiles.keySet()){
                    String lFilename = oFilename.toString();
                    String rFilename = pFiles.getProperty(lFilename);
                    File lParent = new File(baseDir, sAppDir+"/"+lFilename).getParentFile();
                    mgLog.d("copy from assets/appDir/"+rFilename+" to "+sAppDir+"/"+lFilename);
                    assert lParent != null;
                    //noinspection ResultOfMethodCallIgnored
                    lParent.mkdirs();
                    IOUtil.copyStreams(assetManager.open("appDir/"+rFilename), Files.newOutputStream(new File(baseDir, sAppDir + "/" + lFilename).toPath()));
                }

                Properties pPreferences = new Properties();
                pPreferences.load(assetManager.open(wanted+"/preferences.properties"));
                MGMapApplication.loadPropertiesToPreferences(sharedPreferences, pPreferences);
                mode = BaseConfig.Mode.INSTRUMENTATION_TEST;
            }

            BaseConfig baseConfig =  new BaseConfig(sAppDir, preferencesName, sharedPreferences, mode);
            mgLog.d("Setup end: wanted="+wanted+" BaseConfig="+baseConfig);

            application._init(baseConfig);
        } catch (Exception e) {
            mgLog.e(e);
            System.exit(111);
        }
    }

}
