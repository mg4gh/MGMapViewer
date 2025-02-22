package mg.mgmap.generic.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import androidx.core.content.FileProvider;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;

public class BackupUtil {


    private static final String clazz = MethodHandles.lookup().lookupClass().getName();
    private static final MGLog mgLog = new MGLog(clazz);
    private static final String PW = "mfoidfbmiofUUdCos";

    private static final AtomicBoolean inProgress = new AtomicBoolean(false);

    public static String getBackupFileName(boolean latest){
        return "backup_"+(latest?"latest":"full")+".zip";
    }

    public static void checkFullBackup(Activity activity, PersistenceManager persistenceManager){
        long lastFullBackupTime = getFullBackupTime(persistenceManager);
        long days = (System.currentTimeMillis() - lastFullBackupTime) / (1000L*60*60*24L);
        boolean timeTrigger = ( 15000 >= days ) && ( days >= 90L ); // 90 days
        boolean trackNumberTrigger = false;
        ArrayList<File> files = PersistenceManager.getFilesRecursive(persistenceManager.getTrackGpxDir(),PersistenceManager.SUFFIX_GPX);
        int cntNewFiles = 0;
        for (File f : files){
            if (f.isFile() && (f.lastModified() > lastFullBackupTime)) cntNewFiles++;
            if (cntNewFiles > 300){
                trackNumberTrigger = true;
                break;
            }
        }
        String backupFileName = getBackupFileName(false);
        File backupFile = new File(persistenceManager.getBackupDir(), backupFileName);
        boolean noBackupFileTrigger = !backupFile.exists() && (files.size() > 10);
        File backupLatestFile = new File(persistenceManager.getBackupDir(), getBackupFileName(true));
        boolean backupLatestLarge = backupLatestFile.exists() && (backupLatestFile.length() > 20*1000*1000);
        mgLog.d("checkFullBackup timeTrigger="+timeTrigger+" days="+days+" trackNumberTrigger="+trackNumberTrigger+" cntNewFiles="+cntNewFiles+" noBackupFileTrigger="+noBackupFileTrigger+" backupLatestLarge="+backupLatestLarge);
        if (timeTrigger || trackNumberTrigger || noBackupFileTrigger || backupLatestLarge){
            DialogView dialogView = activity.findViewById(R.id.dialog_parent);
            dialogView.lock(() -> dialogView
                    .setTitle("Full GPX Backup")
                    .setMessage("""
                            It's time to make a full backup of gpx tracks. Once the backup_full.zip is created it will be offered via \
                            the Android Share mechanism. It's recommended to use your google drive as a target.\s
                            
                            If you want to restore the archive later, just copy this file via the Android share \
                            mechanism back to the MGMapViewer/backup/restore/ folder and restart the app.""")
                    .setPositive("OK", evt -> trigger(activity, persistenceManager, false))
                    .setNegative("Cancel", null)
                    .show());
        }
    }

    public static void checkLatestBackup(Context context, PersistenceManager persistenceManager){
        String backupFileName = getBackupFileName(true);
        File backupFile = new File(persistenceManager.getBackupDir(), backupFileName);
        long minutes = (System.currentTimeMillis() - backupFile.lastModified()) / (1000L*60L);
        mgLog.d("checkLatestBackup minutes="+minutes+" backupFile.exists="+backupFile.exists());
        if ( (minutes >= 60L) || !backupFile.exists() ){ // 1 hours
            BackupUtil.trigger(context, persistenceManager, true);
        }
    }

    public static void trigger(Context context, PersistenceManager persistenceManager, boolean latest){
        String backupFileName = getBackupFileName(latest);
        mgLog.d("trigger prepare backup");
        if (inProgress.compareAndSet(false, true)){
            BgJob bgJob = new BgJob(){
                @Override
                protected void doJob()  {

                    try {
                        mgLog.d("prepare backup - job started for "+backupFileName);
                        File dataBackupDir = new File(context.getFilesDir(), "backup");
                        if (dataBackupDir.exists()){ // make sure that there is no other stuff which might influence the backup
                            if (!PersistenceManager.forceDelete(dataBackupDir)) mgLog.e("delete failed for dataBackupDir "+dataBackupDir.getAbsolutePath());
                        }
                        if (!dataBackupDir.exists()){
                            if (!dataBackupDir.mkdir()) mgLog.e( "creation of data backup dir failed: "+dataBackupDir.getAbsolutePath());
                        }

                        File backupFile = new File(persistenceManager.getBackupDir(), backupFileName);
                        if (backupFile.exists()){
                            if (!backupFile.delete()) mgLog.e("delete failed for old backupFile: "+backupFile.getAbsolutePath());
                        }
                        File backupFileTemp = new File(persistenceManager.getBackupDir(), backupFileName+".temp");
                        if (backupFileTemp.exists()){
                            if (!backupFileTemp.delete()) mgLog.e("delete failed for old backupFile: "+backupFileTemp.getAbsolutePath());
                        }
                        mgLog.d(clazz+PW);
                        boolean success = false;

                        long lastFullBackupTime = getFullBackupTime(persistenceManager);
                        try (ZipFile zipFile = new ZipFile( backupFileTemp )){
                            ArrayList<File> files = PersistenceManager.getFilesRecursive(persistenceManager.getTrackGpxDir(),PersistenceManager.SUFFIX_GPX);
                            if (files.isEmpty()) return;
                            files.sort( (f1,f2)-> -(Long.compare(f1.lastModified(),f2.lastModified())) );
                            int latestIdx = (latest &&  (files.size() > 365))?365:files.size()-1;
                            while (latest && (latestIdx != 0) && (files.get(latestIdx).lastModified() < lastFullBackupTime)) latestIdx--; // latest backup only files newer than last full backup
                            long latestModified = files.get(latestIdx).lastModified();
                            ZipParameters zipParameters = new ZipParameters();
                            zipParameters.setExcludeFileFilter(file -> file.isDirectory()? PersistenceManager.checkFilesOlderThan(file,latestModified):file.lastModified() < latestModified);
                            zipFile.setRunInThread(true);
                            ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                            zipFile.addFolder(persistenceManager.getTrackGpxDir(), zipParameters);

                            while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                mgLog.d("compress progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone());
                                setProgress( progressMonitor.getPercentDone() / 2 );
                                SystemClock.sleep(1000);
                            }
                            mgLog.d("compress progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" result="+progressMonitor.getResult());
                            if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                mgLog.d("prepare backup - create zip successful for "+backupFileName);
                                success = true;
                            }
                        }

                        if (success){
                            try (ZipFile zipFile = new ZipFile( backupFile, (clazz+PW).toCharArray() )){
                                ZipParameters zipParameters = new ZipParameters();
                                zipParameters.setEncryptFiles(true);
                                zipParameters.setEncryptionMethod(EncryptionMethod.AES);
                                // Below line is optional. AES 256 is used by default. You can override it to use AES 128. AES 192 is supported only for extracting.
                                zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                                zipFile.setRunInThread(true);
                                ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                                zipFile.addFile(backupFileTemp, zipParameters);
                                while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                    mgLog.d("encrypt progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone());
                                    setProgress( 50 + progressMonitor.getPercentDone()/2 );
                                    SystemClock.sleep(1000);
                                }
                                mgLog.d("encrypt progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" result="+progressMonitor.getResult());
                                if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                    if (latest){
                                        mgLog.d("prepare backup - copy to dataBackupDir "+backupFileName+" to "+dataBackupDir.getAbsolutePath());
                                        IOUtil.copyFile( backupFile, new File(dataBackupDir, backupFileName) );
                                    } else { // full backup
                                        triggerFullBackupShare(context, backupFile);
                                    }
                                    File baseDirExt = persistenceManager.getBaseDirExt();
                                    if ((baseDirExt != null) && (persistenceManager.getBaseDir() != baseDirExt)){
                                        mgLog.d("prepare backup - copy "+backupFileName+" to "+baseDirExt.getAbsolutePath());
                                        IOUtil.copyFile( backupFile, new File(baseDirExt, backupFileName) );
                                    }
                                }
                            }
                        }
                        if (backupFileTemp.exists()){
                            if (!backupFileTemp.delete()) mgLog.e("delete failed for old backupFile: "+backupFileTemp.getAbsolutePath());
                        }

                        mgLog.d("prepare backup - job finished for "+backupFileName);
                    } catch (Exception e){
                        mgLog.e(e);
                    } finally {
                        inProgress.set(false);
                    }
                }
            };
            bgJob.setText("Prepare backup of gpx for "+backupFileName);
            bgJob.setMax(100);
            MGMapApplication.getByContext(context).addBgJob(bgJob);
        } else {
            mgLog.d("trigger prepare backup failed - other backup operation in Progress");
        }
    }

    /**
     * Restore from /data folder to restore folder
     */
    public static void restore(Context context, PersistenceManager persistenceManager){
        File dataRestoreMarker = new File(context.getFilesDir(), "dataRestoreMarker");
        try {
            String backupFileName = getBackupFileName(true);
            mgLog.d("restore backup - check started for "+backupFileName);
            File dataBackupDir = new File(context.getFilesDir(), "backup");
            File dataBackupFile = new File(dataBackupDir, backupFileName);
            mgLog.d("restore backup - dataBackupFile="+dataBackupFile.getAbsolutePath()+" dataBackupDir.exists="+dataBackupDir.exists()
                    +" dataBackupFile.exists="+dataBackupFile.exists()+" dataRestoreMarker="+dataRestoreMarker.exists());
            if (!dataRestoreMarker.exists()){
                File backupFile = new File(persistenceManager.getRestoreDir(), backupFileName);
                if (dataBackupDir.exists() && dataBackupFile.exists()){
                    mgLog.d("restore backup - copy started for "+backupFile.getAbsolutePath());
                    IOUtil.copyFile( dataBackupFile, backupFile);
                    mgLog.d("restore backup - copy finished for "+backupFile.getAbsolutePath());
                }
            }
            mgLog.d("restore backup - check finished for "+backupFileName);
        } catch (Exception e) {
            mgLog.e(e);
        } finally {
            try {
                new FileOutputStream(dataRestoreMarker).close(); // touch restore marker
            } catch (IOException e) { mgLog.e(e); }
        }

    }

    public static void restore2(Context context, PersistenceManager persistenceManager, boolean latest){
        String backupFileName = getBackupFileName(latest);
        File backupFile = new File(persistenceManager.getRestoreDir(), backupFileName);
        if (backupFile.exists()){
            if (inProgress.compareAndSet(false, true)) {
                BgJob bgJob = new BgJob(){
                    @Override
                    protected void doJob() {
                        try {
                            mgLog.d("restore backup - job started");
                            if (backupFile.length() > 0){
                                boolean success = false;

                                File backupFileTemp = new File(persistenceManager.getRestoreDir(), backupFileName+".temp");
                                if (backupFileTemp.exists()){
                                    if (!backupFileTemp.delete()) mgLog.e("delete failed for old backupFile: "+backupFileTemp.getAbsolutePath());
                                }
                                try (ZipFile zipFile = new ZipFile( backupFile, (clazz+PW).toCharArray() )){
                                    zipFile.setRunInThread(true);

                                    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                                    zipFile.extractAll(persistenceManager.getRestoreDir().getAbsolutePath());
                                    while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                        mgLog.d("decryp progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                        setProgress(progressMonitor.getPercentDone());
                                        SystemClock.sleep(1000);
                                    }
                                    mgLog.d("decryp progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName()+" result="+progressMonitor.getResult());
                                    if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                        mgLog.d("restore backup - uncompress zip successful for "+backupFileName);
                                        success = true;
                                    }
                                } // try (ZipFile zipFile =...

                                if (success){
                                    File restoreGpxDir = new File(persistenceManager.getRestoreDir(), "gpx");
                                    PersistenceManager.forceDelete(restoreGpxDir); // should not exists, but just in case ...
                                    try (ZipFile zipFile = new ZipFile( backupFileTemp )){
                                        zipFile.setRunInThread(false);
                                        ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                                        zipFile.extractAll(persistenceManager.getRestoreDir().getAbsolutePath());
                                        while (!progressMonitor.getState().equals(ProgressMonitor.State.READY) || (progressMonitor.getResult() == null)) {
                                            mgLog.d("decompress progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName());
                                            setProgress(100);
                                            SystemClock.sleep(100);
                                        }
                                        mgLog.d("decompress progress: "+progressMonitor.getState()+" "+progressMonitor.getWorkCompleted()+" "+progressMonitor.getTotalWork()+" "+progressMonitor.getPercentDone()+" "+progressMonitor.getFileName()+" result="+progressMonitor.getResult());
                                        if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)){
                                            PersistenceManager.mergeDir(restoreGpxDir, persistenceManager.getTrackGpxDir());
                                        }
                                    } // try (ZipFile zipFile =...
                                    if (!PersistenceManager.forceDelete(restoreGpxDir)) mgLog.e("delete failed for restoreGpxDir "+restoreGpxDir.getAbsolutePath());
                                }
                                if (backupFileTemp.exists()){
                                    if (!backupFileTemp.delete()) mgLog.e("delete failed for old backupFile: "+backupFileTemp.getAbsolutePath());
                                }

                            }
                        } catch (IOException e) {
                            mgLog.e(e);
                        } finally {
                            inProgress.set(false);
                            File renameTo = new File(persistenceManager.getRestoreDir(), backupFileName +".done");
                            if (renameTo.exists()){
                                if (!renameTo.delete()) mgLog.e("delete failed for: "+renameTo.getAbsolutePath());
                            }
                            if (!backupFile.renameTo(renameTo)) mgLog.e("rename failed from "+backupFile.getAbsolutePath()+" to "+renameTo.getAbsolutePath() ) ;
                        }
                    }
                };
                bgJob.setText("Restore backup of gpx.");
                bgJob.setMax(100);
                MGMapApplication.getByContext(context).addBgJob(bgJob);
            } else {
                mgLog.d("restore backup failed - other backup operation in Progress");
            }
        }

    }

    private static void triggerFullBackupShare(Context context, File fullBackupFile){
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", fullBackupFile);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setClipData(ClipData.newRawUri("", uri));
        sendIntent.setType("*/zip");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(sendIntent, "Share full backup ..."));
    }

    private static long getFullBackupTime(PersistenceManager persistenceManager ){
        File fFullBackup = new File(persistenceManager.getBackupDir(), getBackupFileName(false));
        return fFullBackup.exists()? fFullBackup.lastModified() : 0;
    }

}
