package mg.mgmap.generic.util.basic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;

public class IOUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        try (is; os){
            byte[] buffer = new byte[8 * 1024];
            try (is; os){
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static void copyFile(File a, File b){
        try {
            copyStreams(new FileInputStream(a), new FileOutputStream(b));
        } catch (IOException e) {
            mgLog.e(e);
        }
    }

    public static byte[] readToByteArray(File file) throws IOException{
        return Files.readAllBytes(file.toPath());
    }
    public static byte[] readToByteArray(InputStream is) throws IOException{
        byte[] buf;
        try (is) {
            int size = is.available();
            buf = new byte[size];
            assert (size <= is.read(buf));
        }
        return buf;
    }
}
