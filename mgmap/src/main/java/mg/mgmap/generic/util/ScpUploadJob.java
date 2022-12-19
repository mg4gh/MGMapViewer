/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.util;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class ScpUploadJob extends BgJob {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    Session session;
    File base;
    String targetPrefix;
    File file;

    public ScpUploadJob(Session session, File base, String targetPrefix, File file) {
        super();
        this.session = session;
        this.base = base;
        this.targetPrefix = targetPrefix;
        this.file = file;
        mgLog.i("base="+base.getAbsolutePath()+" targetPrefix="+targetPrefix+" file="+file.getAbsolutePath());
    }

    @Override
    protected void doJob() throws Exception {
        if (!process()){
            mgLog.i("Job failed for "+file.getName());
        }
    }

    public boolean process() throws Exception{
        synchronized (ScpUploadJob.class){ // Multiple BgJob Instances are serialized by this block - otherwise random exceptions occur on channel.connect();

            mgLog.i("Job start for "+file.getName());
            if (!session.isConnected()) {
                return false;
            }

            Channel channel=session.openChannel("exec");
            String cmd = "scp -t \""+targetPrefix+ file.getAbsolutePath().replace(base.getAbsolutePath(),"").substring(1)+"\"";
            mgLog.d("cmd="+cmd);
            ((ChannelExec)channel).setCommand(cmd);
            OutputStream out=channel.getOutputStream();
            InputStream in=channel.getInputStream();
            channel.connect();

            if (checkAck(in) != 0) {
                return false;
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = file.length();
            String command = "C0644 " + filesize + " \""+file.getName() + "\"\n";
            out.write(command.getBytes());
            out.flush();

            if (checkAck(in) != 0) {
                return false;
            }

            // send a content of file
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0)
                    break;
                out.write(buf, 0, len); // out.flush();
            }
            fis.close();
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                return false;
            }
            out.close();
            channel.disconnect();


            mgLog.i("Job finished for "+file.getName());
        }
        return true;
    }



    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0)
            return b;
        if (b == -1)
            return b;

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                mgLog.e("Error: "+ sb);
            }
            if (b == 2) { // fatal error
                mgLog.e("Fatal: "+ sb);
            }
        }
        return b;
    }

}
