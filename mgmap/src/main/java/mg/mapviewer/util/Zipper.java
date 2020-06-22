package mg.mapviewer.util;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;

//import org.apache.commons.io.FilenameUtils;

public class Zipper
{
    private String password;
    private static final String EXTENSION = "zip";

    public Zipper(String password)
    {
        this.password = password;
    }

    public File pack(String filePath) throws ZipException
    {

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipParameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
        if ((password == null) ||(password.equals(""))){
            zipParameters.setEncryptFiles(false);
        } else {
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
            zipParameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
            zipParameters.setPassword(password);
        }
        String baseFileName = filePath.replaceFirst(".*/", "").replaceFirst("\\.\\w*", "");
//        String baseFileName = FilenameUtils.getBaseName(filePath);
        String destinationZipFilePath = filePath + "." + EXTENSION;
        ZipFile zipFile = new ZipFile(destinationZipFilePath);
        zipFile.addFile(new File(filePath), zipParameters);
        return zipFile.getFile();
    }

    public void unpack(String sourceZipFilePath, String extractedZipFilePath) throws ZipException
    {
        ZipFile zipFile = new ZipFile(sourceZipFilePath);

        if (zipFile.isEncrypted())
        {
            zipFile.setPassword(password);
        }

        zipFile.extractAll(extractedZipFilePath);
    }
}