package mg.mgmap.generic.util;

import android.content.Context;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class BackupUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final String PW = "mfoidfbmiofUUdCos";
    private static final String BACKUP_FILE_NAME = "backup.zip";

    public static ZipParameters getZipParameters(){
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        // Below line is optional. AES 256 is used by default. You can override it to use AES 128. AES 192 is supported only for extracting.
        zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        return zipParameters;
    }
    public static void trigger(Context context, PersistenceManager persistenceManager){
        mgLog.d("trigger");
        new Thread(() -> {
            try {
                mgLog.d("trigger backup started");
                File filesDir = context.getFilesDir();
                File backupDir = new File(filesDir, "backup");
                if (!backupDir.exists()){
                    boolean res = backupDir.mkdir();
                    mgLog.d(backupDir.getAbsolutePath()+" created - res="+res);
                }
                File backupTempFile = new File(persistenceManager.getBackupDir(), BACKUP_FILE_NAME);
                File backupFile = new File(backupDir, BACKUP_FILE_NAME);
                mgLog.d((MethodHandles.lookup().lookupClass().getName()+PW));
                try (ZipFile zipFile = new ZipFile( backupTempFile, (MethodHandles.lookup().lookupClass().getName()+PW).toCharArray() )){
                    zipFile.addFolder(persistenceManager.getTrackGpxDir(), getZipParameters());
                }
                IOUtil.copyStreams(new FileInputStream(backupTempFile), new FileOutputStream(backupFile));
                mgLog.d("trigger backup finished");

            } catch (Exception e) {
                mgLog.e(e);
            }
        }).start();
    }

    public static void restore(Context context, PersistenceManager persistenceManager){
        try {
            mgLog.d("restore backup started");
            File filesDir = context.getFilesDir();
            File backupDir = new File(filesDir, "backup");
            File backupTempFile = new File(backupDir, BACKUP_FILE_NAME);
            File restoreFile = new File(persistenceManager.getRestoreDir(), BACKUP_FILE_NAME);
            if (!restoreFile.exists()){
                new FileOutputStream(restoreFile).close(); // touch restoreFile
                if (backupDir.exists() && backupTempFile.exists()){
                    IOUtil.copyStreams(new FileInputStream(backupTempFile), new FileOutputStream(restoreFile));
                    try (ZipFile zipFile = new ZipFile( restoreFile, (MethodHandles.lookup().lookupClass().getName()+PW).toCharArray() )){
                        zipFile.extractAll(persistenceManager.getTrackGpxDir().getAbsolutePath().replace("/gpx", ""));
                    }
                }
            }
            mgLog.d("restore backup finished.");
        } catch (Exception e) {
            mgLog.e(e);
        }
    }
}
