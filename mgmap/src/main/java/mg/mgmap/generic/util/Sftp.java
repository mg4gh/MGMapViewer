package mg.mgmap.generic.util;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public abstract class Sftp {

    protected static final String PROP_HOSTNAME = "hostname";
    protected static final String PROP_PORT = "port";
    protected static final String PROP_USERNAME = "username";
    protected static final String PROP_PK_FILE = "pkFile";
    protected static final String PROP_PASSPHRASE = "passphrase";
    protected static final String PROP_TARGET_PRFIX = "targetPrefix";
    protected static final String PROP_WIFI = "wifi";

    protected Session session;
    protected ChannelSftp channelSftp;
    protected final Properties props;

    public Sftp(File propFile) throws IOException, JSchException, SftpException{
        props = new Properties();
        if (propFile.exists() && propFile.canRead()) {
            props.load(new FileInputStream(propFile));
            System.out.println("NumberOfProps=" + props.size());
            File pkFile = new File(propFile.getParentFile(), props.getProperty(PROP_PK_FILE));
            init(props.getProperty(PROP_USERNAME), props.getProperty(PROP_HOSTNAME), Integer.parseInt(props.getProperty(PROP_PORT)), pkFile, props.getProperty(PROP_PASSPHRASE, ""));
        } else {
            throw new FileNotFoundException(propFile.getAbsolutePath());
        }

    }

    protected boolean checkPreconditions(){
        return true;
    }

    private void init(String username, String hostname, int port, File id, String passphrase) throws JSchException, SftpException {
        if (checkPreconditions()){
            JSch jSch = new JSch();
            jSch.addIdentity(id.getAbsolutePath(), passphrase);
            session = jSch.getSession(username, hostname, port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            Channel channel=session.openChannel("sftp");
            channel.connect();
            channelSftp=(ChannelSftp)channel;
            channelSftp.cd(props.getProperty(PROP_TARGET_PRFIX));
        }
    }

    public void copy() throws Exception{
        if ((session != null) && (channelSftp != null)){
            doCopy();
        }
    }

    protected abstract void doCopy() throws Exception;

    public void close(){
        if (channelSftp != null){
            channelSftp.disconnect();
        }
        if (session != null){
            session.disconnect();
        }
    }

}
