package mg.mgmap.generic.util.basic;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mg.mgmap.application.MGMapApplication;

public class IOUtil {

    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        try (is; os){
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
}
