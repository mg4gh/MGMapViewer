package mg.mgmap.activity.mgmap.features.shareloc;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

import mg.mgmap.generic.util.basic.IOUtil;

public class EncryptionTest {

    @Test
    public void _01_asymmetric() throws Exception {
        File base = new File(".");
        System.out.println(base.getAbsolutePath());
        File fKey = new File("src/main/assets/client.key");
        File fCrt = new File("src/main/assets/client.crt");
        System.out.println(fKey.getAbsolutePath());
        System.out.println(fKey.exists());
        System.out.println(fCrt.getAbsolutePath());
        System.out.println(fCrt.exists());

        SharePerson client = CryptoUtils.getPersonData(new FileInputStream(fCrt));

        String message = "Hello from the other side.";
        System.out.println(message);

        String cryptoMessage = CryptoUtils.encrypt(message, client);
        System.out.println(cryptoMessage);

        byte[] baKey = IOUtil.readToByteArray(fKey);
        String decryptedMessage = CryptoUtils.decrypt(cryptoMessage, baKey);
        System.out.println("decryptedMessage: "+decryptedMessage);

    }

}
