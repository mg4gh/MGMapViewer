package mg.mgmap.generic.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

public class SshBuild {

    /**
     * @param args
     * 1.) sftp.properties
     * 2.) target 2nd prefix
     * 3.) cleanup regex
     * 4..n) file(s)ToCopy
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        System.out.println("Hello from SshBuild " + args.length);
        for (String arg : args) {
            System.out.println(arg);
        }
        try {
            new Sftp(new File(args[0])) {
                @Override
                protected void doCopy() throws SftpException {
                    System.out.println("remote pwd: "+channelSftp.pwd());
                    channelSftp.cd(args[1]);
                    System.out.println("remote pwd: "+channelSftp.pwd());

                    Vector<ChannelSftp.LsEntry> vLsEntries = channelSftp.ls(channelSftp.pwd());
                    for (ChannelSftp.LsEntry lsEntry : vLsEntries){
                        if (!lsEntry.getAttrs().isDir() && lsEntry.getFilename().matches(args[2])){
                            System.out.println("remove old entry: "+lsEntry.getFilename());
                            channelSftp.rm(lsEntry.getFilename());
                        }
                    }
                    ArrayList<File> files = new ArrayList<>();
                    for (int i = 3; i < args.length; i++) {
                        File f = new File(args[i]);
                        if (f.exists() && f.canRead()) {
                            files.add(f);
                        }
                    }
                    props.list(System.out);
                    System.out.println(files);


                    for (File file : files){
                        channelSftp.put(file.getAbsolutePath(), file.getName());
                    }
                }
            }.doCopy();


        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }



}
