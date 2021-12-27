package mg.mgmap.generic.util;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.NameUtil;

public class SshSyncUtil {

    private static final String SYNC_CONFIG = "sync.cfg";
    private static final String SSH_PK_SUB_DIR = "tokens";

    long lastAction = 0;
    boolean syncInProgress = false;

    boolean checkWLAN(MGMapApplication application, String wlanSSID){
        if (wlanSSID == null) return false;
        String ssid = "";
        try {
            WifiManager wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo;

            wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                ssid = wifiInfo.getSSID();
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" current ssid=\""+ssid+"\"");
        return wlanSSID.equals(ssid);
    }

    public void trySynchronisation(MGMapApplication application) {
        long now = System.currentTimeMillis();
        if ((now -lastAction > 10 * 60 * 1000) && (!syncInProgress)){ // no sync, if there was one in the last 10 minutes or another sync is in progress
            new Thread(){
                @Override
                public void run() {
                    trySynchronisationAsync(application);
                }
            }.start();
        }
    }

    private void trySynchronisationAsync(MGMapApplication application){
        try {
            PersistenceManager persistenceManager = application.getPersistenceManager();
            Properties props = persistenceManager.getConfigProperties(null,SYNC_CONFIG);

            String pkFile = props.getProperty("pkFile", "id_rsa");
            String passphrase = props.getProperty("passphrase", "");
            String hostname = props.getProperty("hostname", "localhost");
            short port = Short.parseShort( props.getProperty("port", "22") );
            String username = props.getProperty("username", "user");
            String targetPrefix = props.getProperty("targetPrefix", "mgmap/");
            String wlanSSID = props.getProperty("wlanSSID", "MeinWLAN");

            if (checkWLAN(application, wlanSSID)){

                File pkDir = new File(persistenceManager.getConfigDir(), SSH_PK_SUB_DIR);

                JSch jSch = new JSch();
                jSch.addIdentity(new File(pkDir, pkFile).getAbsolutePath(),passphrase);

                Session session=jSch.getSession(username, hostname, port);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();


                File localFolder = persistenceManager.getTrackGpxDir();
                Map<String, Long> localMap = new HashMap<>();
                calcLocalMap(localMap, localFolder, localFolder.getAbsolutePath(), ".*\\.gpx");

                Map<String, Long> remoteMap = new HashMap<>();
                if (calcRemoteList(session, remoteMap, targetPrefix)){

                    Set<String> commonSet = new TreeSet<>(localMap.keySet());
                    commonSet.retainAll(remoteMap.keySet());
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" remoteSet: "+remoteMap.keySet());
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" commonSet: "+commonSet);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" localSet: "+localMap.keySet());

                    for (String commonName : new TreeSet<>(commonSet)){
                        long localTime = localMap.get(commonName);
                        long remoteTime = remoteMap.get(commonName);
                        if (localTime != remoteTime){
                            if (localTime > remoteTime){
                                remoteMap.remove(commonName); // ignore remote
                            } else {
                                localMap.remove(commonName); // ignore local
                            }
                            commonSet.remove(commonName);
                        }
                    }

                    ArrayList<BgJob> jobs = new ArrayList<>();
                    for (String name : localMap.keySet()){
                        if (!commonSet.contains(name)) {
                            File f = new File(localFolder.getAbsolutePath()+"/"+name);
                            jobs.add(new ScpUploadJob(session, localFolder, targetPrefix, f ));
                        }
                    }


                    String message = "SSH Sync Overview: \ntracks in sync: "+commonSet.size()+" \ntracks to upload: "+jobs.size();
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+message);
                    application.addBgJobs(jobs);
                }

                while (application.numBgJobs() > 0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                    }
                }
                session.disconnect();
            } // fi - is connected to the right WLAN


        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }

    private void calcLocalMap(Map<String, Long> map, File dir, String prefix, String match){
        for (File f : dir.listFiles()){
            if (f.isDirectory()){
                calcLocalMap(map, f, prefix, match);
            } else {
                if (f.getName().matches(match)){
                    String s1 = f.getAbsolutePath().replace(prefix, "");
                    if (s1.startsWith("/") || s1.startsWith("\\")){
                        s1 = s1.substring(1);
                    }
                    map.put(s1, f.lastModified()/1000);
                }
            }
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
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            channel.disconnect();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            return false;
        }
        return true;
    }

}
