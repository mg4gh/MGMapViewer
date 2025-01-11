package mg.mgmap.generic.util;

import android.content.Context;
import android.os.SystemClock;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class BackupUtil {


    private static final String clazz = MethodHandles.lookup().lookupClass().getName();
    private static final MGLog mgLog = new MGLog(clazz);
    private static final String PW = "mfoidfbmiofUUdCos";
    private static final String BACKUP_FILE_NAME1 = "inner_backup.zip";
    private static final String BACKUP_FILE_NAME2 = "backup.zip";

    private static final AtomicBoolean inProgress = new AtomicBoolean(false);

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
        if (inProgress.compareAndSet(false, true)){
            BgJob bgJob = new BgJob(){
                @Override
                protected void doJob() throws Exception {

                    try {
                        boolean success = false;
                        mgLog.d("trigger backup started");
                        File filesDir = context.getFilesDir();
                        File backupDir = new File(filesDir, "backup");
                        if (!backupDir.exists()){
                            boolean res = backupDir.mkdir();
                            mgLog.d(backupDir.getAbsolutePath()+" created - res="+res);
                        }
                        File backupTempFile1 = new File(persistenceManager.getBackupDir(), BACKUP_FILE_NAME1);
                        if (backupTempFile1.exists()){
                            boolean res = backupTempFile1.delete();
                            mgLog.d("delete old backupTempFile1 ("+backupTempFile1.getAbsolutePath()+") - res="+res);
                        }
                        File backupFile = new File(backupDir, BACKUP_FILE_NAME2);
                        mgLog.d(clazz+PW);
                        try (ZipFile zipFile = new ZipFile( backupTempFile1)){
                            zipFile.setRunInThread(true);
                            ProgressMonitor progressMonitor = zipFile.getProgressMonitor();

                            ArrayList<File> files = new ArrayList<>(Arrays.asList(persistenceManager.getTrackGpxDir().listFiles()));
                            files.sort( (f1,f2)-> -(Long.compare(f1.lastModified(),f2.lastModified())) );
                            while (files.size() > 365) files.remove(files.size()-1);
                            zipFile.addFiles(files);
//                            zipFile.addFolder(persistenceManager.getTrackGpxDir());
                            while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                mgLog.d("progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                setProgress(progressMonitor.getPercentDone()/2); // this task is just the first half
                                SystemClock.sleep(1000);
                            }
                            if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                mgLog.d("trigger backup compress: success");
                                success = true;
                            }
                        }
                        if (success){
                            File backupTempFile2 = new File(persistenceManager.getBackupDir(), BACKUP_FILE_NAME2);
                            if (backupTempFile2.exists()){
                                boolean res = backupTempFile2.delete();
                                mgLog.d("delete old backupTempFile2 ("+backupTempFile2.getAbsolutePath()+") - res="+res);
                            }
                            try (ZipFile zipFile = new ZipFile( backupTempFile2, (clazz+PW).toCharArray() )) {
                                zipFile.setRunInThread(true);
                                ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                                zipFile.addFile(backupTempFile1, getZipParameters());
                                while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                    mgLog.d("progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                    setProgress(50+progressMonitor.getPercentDone()/2); // second half of job
                                    SystemClock.sleep(1000);
                                }
                                if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                    mgLog.d("trigger backup encryption: success");
                                    IOUtil.copyStreams(new FileInputStream(backupTempFile2), new FileOutputStream(backupFile));
                                }
                            }
                        }
                        mgLog.d("trigger backup finished");
                    } finally {
                        inProgress.set(false);
                    }
                }
            };
            bgJob.setText("Prepare backup of gpx.");
            bgJob.setMax(100);
            MGMapApplication.getByContext(context).addBgJob(bgJob);
        } else {
            mgLog.d("trigger failed - in Progress");
        }
    }

    /**
     * Restore from /data folder to restore folder
     */
    public static void restore(Context context, PersistenceManager persistenceManager){
        try {
            mgLog.d("restore backup - check started");
            File filesDir = context.getFilesDir();
            File backupDir = new File(filesDir, "backup");
            File backupFile = new File(backupDir, BACKUP_FILE_NAME2);
            File backupTempFile2 = new File(persistenceManager.getRestoreDir(), BACKUP_FILE_NAME2);
            if (!backupTempFile2.exists()){
                new FileOutputStream(backupTempFile2).close(); // touch restoreFile
                if (backupDir.exists() && backupFile.exists()){
                    mgLog.d("restore backup - copy backup file.");
                    IOUtil.copyStreams(new FileInputStream(backupFile), new FileOutputStream(backupTempFile2));
                    mgLog.d("restore backup - copy backup file finished.");
                    new FileOutputStream(new File(persistenceManager.getRestoreDir(), "restore.job")).close();
                }
            }
            mgLog.d("restore backup - check finished.");
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    public static void restore2(Context context, PersistenceManager persistenceManager){
        File restoreJob = new File(persistenceManager.getRestoreDir(), "restore.job");
        if (restoreJob.exists()){
            BgJob bgJob = new BgJob(){
                @Override
                protected void doJob() {
                    boolean success = false;
                    try {
                        mgLog.d("restore backup - job started");
                        File backupTempFile2 = new File(persistenceManager.getRestoreDir(), BACKUP_FILE_NAME2);
                        try (ZipFile zipFile = new ZipFile( backupTempFile2, (clazz+PW).toCharArray() )){
                            zipFile.setRunInThread(true);
                            ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                            zipFile.extractAll(persistenceManager.getRestoreDir().getAbsolutePath());
                            while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                mgLog.d("progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                setProgress(progressMonitor.getPercentDone()/2); // this task is just the first half
                                SystemClock.sleep(1000);
                            }
                            mgLog.d("trigger backup decryption: "+progressMonitor.getResult());
                            if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                success = true;
                            }
                        }
                        if (success){
                            File backupTempFile1 = new File(persistenceManager.getRestoreDir(), BACKUP_FILE_NAME1);
                            try (ZipFile zipFile = new ZipFile( backupTempFile1 )){
                                zipFile.setRunInThread(true);
                                ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                                zipFile.extractAll(persistenceManager.getTrackGpxDir().getAbsolutePath().replace("/gpx", ""));
                                while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                    mgLog.d("progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                    setProgress(50+progressMonitor.getPercentDone()/2); // this task is just the first half
                                    SystemClock.sleep(1000);
                                }
                                mgLog.d("trigger backup uncompress: "+progressMonitor.getResult());
                            }

                        }
                    } catch (IOException e) {
                        mgLog.e(e);
                    } finally {
                        boolean res = restoreJob.delete();
                        mgLog.d("restore.job deleted - res="+res);
                    }

                }
            };
            bgJob.setText("Restore backup of gpx.");
            bgJob.setMax(100);
            MGMapApplication.getByContext(context).addBgJob(bgJob);
        }

    }

}
