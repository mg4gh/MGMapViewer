package mg.mgmap.mgmain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class CryptoUtils {

    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encrypt(String data, byte[] key) throws Exception {
        if (key.length != 32) {
            throw new IllegalArgumentException("Key must be 32 bytes for AES-256");
        }
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[12]; // standard IV size for GCM
        secureRandom.nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Return IV + encrypted data encoded in Base64
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String base64Data, byte[] key) throws Exception {
        if (key.length != 32) {
            throw new IllegalArgumentException("Key must be 32 bytes for AES-256");
        }
        byte[] combined = Base64.getDecoder().decode(base64Data);

        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        byte[] encrypted = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static byte[] createAESKey(UUID uuid1, UUID uuid2) {
        byte[] key = new byte[32];
        System.arraycopy(convertUUIDToBytes(uuid1), 0, key, 0, 16);
        System.arraycopy(convertUUIDToBytes(uuid2), 0, key, 16, 16);
        return key;
    }

    public static SSLSocketFactory getSocketFactory(InputStream caCrt, InputStream clientCrt, InputStream clientKey) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caCrt);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-cert", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        X509Certificate cert = (X509Certificate) cf.generateCertificate(clientCrt);
        PrivateKey key = loadPrivateKey(clientKey);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", cert);
        keyStore.setKeyEntry("client-key", key, "password".toCharArray(), new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.3");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    public static PrivateKey loadPrivateKey(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("-----BEGIN") && !line.startsWith("-----END")) {
                    sb.append(line.trim());
                }
            }
        }
        byte[] encoded = Base64.getDecoder().decode(sb.toString());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
