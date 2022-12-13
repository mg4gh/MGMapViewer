package mg.mgmap.generic.util;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.NameUtil;

public class SshSyncUtil {

    private static final String SYNC_CONFIG = "sync.cfg";
    private static final String SSH_PK_SUB_DIR = "tokens";

    long lastAction = 0;
    boolean syncInProgress = false;
    
    public void trySynchronisation(MGMapApplication application) {
        long now = System.currentTimeMillis();
        if ((now -lastAction > 10 * 60 * 1000) && (!syncInProgress)){ // no sync, if there was one in the last 10 minutes or another sync is in progress
            new Thread(() -> trySynchronisationAsync(application)).start();
        }
    }

    private void trySynchronisationAsync(MGMapApplication application){
        try {
            PersistenceManager persistenceManager = application.getPersistenceManager();
            Properties props = persistenceManager.getConfigProperties(null,SYNC_CONFIG);
            if (props.size() == 0) return;

            String pkFile = props.getProperty("pkFile", "id_rsa");
            String passphrase = props.getProperty("passphrase", "");
            String hostname = props.getProperty("hostname", "localhost");
            short port = Short.parseShort( props.getProperty("port", "22") );
            String username = props.getProperty("username", "user");
            String targetPrefix = props.getProperty("targetPrefix", "mgmap/");
            String wlanSSID = props.getProperty("wlanSSID", "MeinWLAN");

            if (WiFiUtil.checkWLAN(application, wlanSSID)){

                File pkDir = new File(persistenceManager.getConfigDir(), SSH_PK_SUB_DIR);

                JSch jSch = new JSch();
                jSch.addIdentity(new File(pkDir, pkFile).getAbsolutePath(),passphrase);

                Session session=jSch.getSession(username, hostname, port);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();


                File localFolder = persistenceManager.getTrackGpxDir();
                try (Stream<Path> walk = Files.walk(localFolder.toPath())){
                    Map<String, Long> localMap = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toFile().getName().matches(".*\\.gpx"))
                            .collect(Collectors.toMap(p -> p.toFile().getAbsolutePath().replaceFirst(localFolder.getAbsolutePath()+"[/\\\\]",""), p -> p.toFile().lastModified()/1000));

                    Map<String, Long> remoteMap = new HashMap<>();
                    if (calcRemoteList(session, remoteMap, targetPrefix)){

                        Set<String> commonSet = new TreeSet<>(localMap.keySet());
                        commonSet.retainAll(remoteMap.keySet());
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" remoteSet: "+remoteMap.keySet());
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" commonSet: "+commonSet);
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" localSet: "+localMap.keySet());
                        int total = localMap.size();

                        for (String commonName : new TreeSet<>(commonSet)){
                            Long localTime = localMap.get(commonName);
                            Long remoteTime = remoteMap.get(commonName);

                            if ((localTime != null) && (remoteTime != null) && (!localTime.equals(remoteTime))){
                                if (localTime > remoteTime){
                                    remoteMap.remove(commonName); // ignore remote
                                } else {
                                    localMap.remove(commonName); // ignore local
                                }
                                commonSet.remove(commonName);
                            }
                        }

                        BgJobGroup bgJobGroup = new BgJobGroup(application, null, null, new BgJobGroupCallback() {
                            @Override
                            public boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                                session.disconnect();
                                return false;
                            }
                        });
                        for (String name : localMap.keySet()){
                            if (!commonSet.contains(name)) {
                                File f = new File(localFolder.getAbsolutePath()+"/"+name);
                                bgJobGroup.addJob(new ScpUploadJob(session, localFolder, targetPrefix, f ));
                            }
                        }
                        String message = "SSH Sync Overview: tracks in sync: "+(total-bgJobGroup.size())+" tracks to upload: "+bgJobGroup.size();
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+message);
                        bgJobGroup.setConstructed(null);
                    } // if (calcRemoteList(session, remoteMap, targetPrefix)){
                } // try (Stream<Path> walk = Files.walk(localFolder.toPath())){
            } // fi - is connected to the right WLAN
        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }

    public boolean calcRemoteList(Session session, Map<String, Long> map, String targetPrefix){
        try {
            Channel channel=session.openChannel("exec");

            String cmd = "bash -c \"find "+targetPrefix+" -type f -print | sed 's/\\(.*\\)/\\\"\\1\\\"/' | xargs stat -c %Y:%n\"";
            ((ChannelExec)channel).setCommand(cmd);

            InputStream in=channel.getInputStream();
            channel.connect();

            long to = System.currentTimeMillis()+5000;

            String line;
            BufferedReader bin = new BufferedReader(new InputStreamReader(in));
            while (System.currentTimeMillis() < to){
                boolean exit = (channel.getExitStatus() >= 0);
                if (in.available() > 0) {
                    while ((line = bin.readLine()) != null) {
                        Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+line);
                        map.put(line.replaceFirst(".*:"+targetPrefix,""), Long.parseLong(line.replaceFirst(":.*","")));
                    }
                }
                boolean exit2 = (channel.getExitStatus() >= 0);

                if (exit || exit2) break;
                WaitUtil.doWait(this.getClass(), 20, MGMapApplication.LABEL);
            }
            channel.disconnect();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            return false;
        }
        return true;
    }

}
