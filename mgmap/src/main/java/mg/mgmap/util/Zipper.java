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
package mg.mgmap.util;

import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import mg.mgmap.MGMapApplication;

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
        String destinationZipFilePath = filePath + "." + EXTENSION;

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(CompressionMethod.DEFLATE );
        zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
        ZipFile zipFile;
        if ((password == null) ||(password.equals(""))){
            zipParameters.setEncryptFiles(false);
            zipFile = new ZipFile(destinationZipFilePath);
        } else {
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            zipFile = new ZipFile(destinationZipFilePath, password.toCharArray());
        }
        zipFile.addFile(new File(filePath), zipParameters);
        return zipFile.getFile();
    }

    public void unpack(String sourceZipFilePath, String extractedZipFilePath) throws ZipException {
        ZipFile zipFile = new ZipFile(sourceZipFilePath);

        if (zipFile.isEncrypted())
        {
            zipFile.setPassword(password.toCharArray());
        }
        zipFile.extractAll(extractedZipFilePath);
    }

    public void unpack(URL url, File extractedZipFilePath, FilenameFilter filter, BgJob bgJob) throws Exception {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" extract url="+url+" extractedZipFilePath="+extractedZipFilePath);
        URLConnection connection = url.openConnection();
        connection.connect();
        bgJob.setMax((int)(connection.getContentLengthLong()/1000));
        bgJob.setText("Download "+ url.toString().replaceFirst(".*/",""));
        bgJob.setProgress(0);
//        bgJob.notifyUser();

        InputStream inputStream = url.openStream();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream, (password==null)?null:password.toCharArray());
        LocalFileHeader localFileHeader;
        int readLen;
        long totalLength = 0;

        byte[] readBuffer = new byte[4096];

        Log.i(MGMapApplication.LABEL, NameUtil.context()+" extractedZipFilePath="+extractedZipFilePath);
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {

            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+localFileHeader.getFileName());
            long fileLengthC = localFileHeader.getCompressedSize();
            long fileLengthU = localFileHeader.getUncompressedSize();
            double compressionFactor = fileLengthC / (double)fileLengthU;
            totalLength += fileLengthC;
            long fileRead = 0;

            OutputStream outputStream = null;
            try {
                File extractedFile = new File(extractedZipFilePath, localFileHeader.getFileName());
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" extractedFile="+extractedFile);
                if ((filter==null) || ( filter.accept( extractedFile.getParentFile(), extractedFile.getName() ))){

                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" extractedFile="+extractedFile);
                    if (localFileHeader.isDirectory()){
                        boolean res = extractedFile.mkdirs();
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" extractedFile="+extractedFile+" dir created "+res);
                    } else {
                        outputStream = new FileOutputStream(extractedFile);
                        while ((readLen = zipInputStream.read(readBuffer)) != -1) {
                            outputStream.write(readBuffer, 0, readLen);
                            fileRead += readLen;
                            if (bgJob != null){
                                long value = totalLength-fileLengthC+ (long)(fileRead*compressionFactor);
                                bgJob.setProgress((int)(value/1000));
//                                bgJob.notifyUser();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.e (MGMapApplication.LABEL, NameUtil.context(),e);
            } finally {
                try {
                    if (outputStream != null){
                        outputStream.close();
                    }
                } catch (Exception e){
                    Log.e (MGMapApplication.LABEL, NameUtil.context(),e);
                }
            }
        }
    }
}