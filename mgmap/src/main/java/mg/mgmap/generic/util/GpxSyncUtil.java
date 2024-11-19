package mg.mgmap.generic.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.MGLog;

public class GpxSyncUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final String SYNC_CONFIG = "gpx_sync.properties";

    final long lastAction = 0;
    final boolean syncInProgress = false;
    
    public void trySynchronisation(MGMapApplication application) {
        long now = System.currentTimeMillis();
        if ((now -lastAction > 10 * 60 * 1000) && (!syncInProgress)){ // no sync, if there was one in the last 10 minutes or another sync is in progress
            new Thread(() -> trySynchronisationAsync(application)).start();
        }
    }

    private void trySynchronisationAsync(MGMapApplication application){
        try {
            PersistenceManager persistenceManager = application.getPersistenceManager();
            File syncProps = new File(persistenceManager.getConfigDir(), SYNC_CONFIG);
            if (!syncProps.exists()) return;

            new Sftp(syncProps) {
                @Override
                protected boolean checkPreconditions() {
                    return (WiFiUtil.checkWLAN(application, props.getProperty(PROP_WIFI)));
                }

                @Override
                protected void doCopy() throws IOException, SftpException {


                    File localFolder = persistenceManager.getTrackGpxDir();
                    channelSftp.lcd(localFolder.getAbsolutePath());
                    try (Stream<Path> walk = Files.walk(localFolder.toPath())) {
                        Map<String, Long> localMap = walk
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toFile().getName().endsWith(".gpx"))
                                .collect(Collectors.toMap(p -> p.toFile().getAbsolutePath().replaceFirst(localFolder.getAbsolutePath() , ""), p -> p.toFile().lastModified() / 1000));

                        Map<String, Long> remoteMap = new HashMap<>();
                        calcRemoteMap(channelSftp, remoteMap, File.separator);

                        Set<String> commonSet = new TreeSet<>(localMap.keySet());
                        commonSet.retainAll(remoteMap.keySet());
                        mgLog.i("remoteSet: " + remoteMap.keySet());
                        mgLog.i("commonSet: " + commonSet);
                        mgLog.i("localSet: " + localMap.keySet());

                        for (String commonName : new TreeSet<>(commonSet)) {
                            Long localTime = localMap.get(commonName);
                            Long remoteTime = remoteMap.get(commonName);

                            if ((localTime != null) && (remoteTime != null) && (!localTime.equals(remoteTime))) {
                                if (localTime > remoteTime) {
                                    remoteMap.remove(commonName); // ignore remote
                                } else {
                                    localMap.remove(commonName); // ignore local
                                }
                                commonSet.remove(commonName);
                            }
                        }

                        for (String name : localMap.keySet()) {
                            if (!commonSet.contains(name)) {
                                String[] paths = name.split("/");
                                String curPath=".";
                                for (int i=1; i< paths.length-1; i++){
                                    curPath += File.separator + paths[i];
                                    try {
                                        channelSftp.stat(curPath);
                                    } catch (SftpException e) {
                                        mgLog.d(String.format("Create remote dir %s in %s",curPath, channelSftp.pwd()));
                                        channelSftp.mkdir(curPath);
                                    }
                                }
                                mgLog.d(String.format("Sftp put %s%s to %s%s",channelSftp.lpwd(),name,channelSftp.pwd(),name));
                                name = name.substring(1); // remove leading File.separator
                                channelSftp.put(name, name);
                            }
                        }
                    } // try (Stream<Path> walk = Files.walk(localFolder.toPath())){
                }
            }.copy();
        } catch (Exception e){
            mgLog.e(e);
        }
    }

    public void calcRemoteMap(ChannelSftp channelSftp, Map<String, Long> map, String subPath) throws SftpException{
        Vector<ChannelSftp.LsEntry> vLsEntries = channelSftp.ls(channelSftp.pwd()+subPath);
        for (ChannelSftp.LsEntry lsEntry : vLsEntries){
            if (lsEntry.getAttrs().isDir()){
                if (!lsEntry.getFilename().equals(".") && !lsEntry.getFilename().equals("..")){
                    calcRemoteMap(channelSftp, map, subPath+lsEntry.getFilename()+File.separator);
                }
            } else if (lsEntry.getFilename().endsWith(".gpx")){
                map.put(subPath+lsEntry.getFilename(), (long)lsEntry.getAttrs().getMTime()) ;
            }
        }
    }
}
