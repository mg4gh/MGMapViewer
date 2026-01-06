package mg.mgmap.mgmain;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MgMain {

    private static class RegistrationRecord {
        String clientId;
        String email;
        String confirmationId;
        long timestamp;

        public RegistrationRecord(String clientId, String email, String confirmationId) {
            this.clientId = clientId;
            this.email = email;
            this.confirmationId = confirmationId;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "RegistrationRecord{" +
                    "clientId='" + clientId + '\'' +
                    ", email='" + email + '\'' +
                    ", confirmationId='" + confirmationId + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }

    }

    private static final Map<String, RegistrationRecord> registrations = new ConcurrentHashMap<>();
    private static MqttClient mqttClient;

    public static void main(String[] args) {
        new MgMain();
    }

    int cnt = 0;

    public MgMain(){
        System.out.println("Hello from MgMain - working directory = "+new File(".").getAbsolutePath());

        startCleanupTimer();

        try {
            long now = System.currentTimeMillis();

            String broker = "ssl://kmt6pewiw8ty93su.myfritz.net:9999";
            String clientId = "MgMainSubscriber_" + UUID.randomUUID();

            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();

            try (InputStream caCrt = new FileInputStream("certs/ca.crt");
                 InputStream clientCrt = new FileInputStream("certs/server.crt");
                 InputStream clientKey = new FileInputStream("certs/server.key")) {

                options.setSocketFactory(CryptoUtils.getSocketFactory(caCrt, clientCrt, clientKey));
            }

            options.setCleanStart(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    System.err.println("Disconnected: " + disconnectResponse.getReasonString());
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    System.err.println("MQTT error: " + exception.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("Message arrived. Topic: " + topic + " Payload: " + payload);

                    if (topic.equals("/client/server/register_init")) {
                        handleRegisterInit(payload);
                    } else if (topic.equals("/client/server/register_request")) {
                        handleRegisterRequest(payload);
                    } else if (topic.endsWith("/server/register_verify")) {
                        handleRegisterVerify(topic);
                    } else if (topic.endsWith("/server/crt_request")) {
                        handleCrtRequest(topic, payload);
                    } else if (topic.endsWith("/server/unregister_request")) {
                        handleUnregisterRequest(topic, payload);
                    } else if (topic.equals("/server/server/watchdog")) {
                        SystemdNotify.watchdog();
                        System.out.println("Watchdog received");
                    }
                }

                @Override
                public void deliveryComplete(IMqttToken token) {}

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println("Connect complete. Reconnect: " + reconnect);
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe("/+/server/#", 1);
            System.out.println("Subscribed to /+/server/# after "+(System.currentTimeMillis()-now)+"ms");

            sendMqttMessage("/server/server/init", "init");

            SystemdNotify.ready();


            while (true) {
                synchronized (this){
                    this.wait(10000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void startCleanupTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                registrations.entrySet().removeIf(entry -> entry.getValue().timestamp < oneHourAgo);
                System.out.println("Cleanup performed. Remaining registrations: " + registrations.size());
                try {
                    sendMqttMessage("/server/server/watchdog", "watchdog "+cnt++);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }, 2 * 60 * 1000, 2 * 60 * 1000);
    }

    private void handleRegisterInit(String payload) {
        String[] payloadParts = payload.split(":", 2);
        if (payloadParts.length == 2) {
            String senderClientId = payloadParts[0];
            String email = payloadParts[1];
            
            System.out.println("REGISTRATION RECEIVED:");
            System.out.println("Client ID: " + senderClientId);
            System.out.println("Email: " + email);

            String confirmationId = UUID.randomUUID().toString().substring(0,8);

            String emailError = MailUtil.sendEmail(email, confirmationId);
            boolean success = (emailError == null);
            String info = success ? "Mail sent successfully" : emailError;
            
            if (success) {
                RegistrationRecord record = new RegistrationRecord(senderClientId, email, confirmationId);
                registrations.put(senderClientId, record);
                System.out.println("Stored registration record for client: " + senderClientId);
            }

            String responsePayload = success + ":" + info;

            try {
                sendMqttMessage("/server/" + senderClientId + "/register_confirm", responsePayload);
                System.out.println("Sent confirmation to: " + senderClientId);
            } catch (Exception e) {
                System.err.println("Failed to send MQTT confirmation: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid payload format for registration init: " + payload);
        }
    }

    private void handleRegisterVerify(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            String email = parts[1];
            try {
                sendMqttMessage("/server/" + email + "/verify_confirm", "success");
                System.out.println("Sent verify confirm to: " + email);
            } catch (Exception e) {
                System.err.println("Failed to send MQTT verify confirm: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleRegisterRequest(String payload) {
        String[] payloadParts = payload.split(":", 3);
        if (payloadParts.length != 3) {
            System.err.println("Invalid payload format for registration request: " + payload);
            return;
        }

        String clientId = payloadParts[0];
        String hashedConfirmationUuid = payloadParts[1];
        String pemCsr = payloadParts[2];

        String extra1 = "success";
        String extra2;

        RegistrationRecord record = registrations.get(clientId);
        try {
            if (record == null) {
                throw new Exception("No record found for clientId: " + clientId);
            }
            if (!CryptoUtils.sha256(record.confirmationId).equals(hashedConfirmationUuid)) {
                throw new Exception("Confirmation code mismatch");
            }

            System.out.println("REGISTRATION REQUEST VERIFIED for client: " + clientId);

            // Load CA cert and key
            X509Certificate caCert;
            PrivateKey caPrivateKey;
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            try (InputStream isCrt = new FileInputStream("certs/ca.crt");
                 InputStream isKey = new FileInputStream("certs/ca.key")) {
                caCert = (X509Certificate) certFactory.generateCertificate(isCrt);
                caPrivateKey = CryptoUtils.loadPrivateKey(isKey);

                // Parse CSR
                PKCS10CertificationRequest csr;
                try (PEMParser pemParser = new PEMParser(new StringReader(pemCsr))) {
                    Object obj = pemParser.readObject();
                    if (obj instanceof PKCS10CertificationRequest) {
                        csr = (PKCS10CertificationRequest) obj;
                    } else {
                        throw new Exception("Invalid CSR format");
                    }
                }

                JcaPKCS10CertificationRequest jcaCsr = new JcaPKCS10CertificationRequest(csr);

                // Verify that the CN in the CSR matches the registered email
                X500Name subject = jcaCsr.getSubject();
                RDN[] rdns = subject.getRDNs(BCStyle.CN);
                if (rdns.length == 0) {
                    throw new Exception("CSR does not contain a Common Name (CN)");
                }
                String cn = IETFUtils.valueToString(rdns[0].getFirst().getValue());
                if (!cn.equals(record.email)) {
                    throw new Exception("CSR CN mismatch. Expected: " + record.email + ", Found: " + cn);
                }
                
                // Sign Certificate
                BigInteger certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.YEAR, 1);
                Date endDate = calendar.getTime();

                X500Name issuerName = new X500Name(caCert.getSubjectX500Principal().getName());

                X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                        issuerName, certSerialNumber, new Date(), endDate, jcaCsr.getSubject(), jcaCsr.getPublicKey());

                ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(caPrivateKey);
                X509Certificate clientCertificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));

                // Convert Certificate to PEM string
                StringWriter certWriter = new StringWriter();
                try (JcaPEMWriter pemWriter = new JcaPEMWriter(certWriter)) {
                    pemWriter.writeObject(clientCertificate);
                }
                extra2 = certWriter.toString();
                System.out.println("Generated and signed certificate for: " + clientId);

            }
            registrations.remove(clientId);
        } catch (Exception e) {
            System.err.println("Registration request failed: " + e.getMessage());
            extra1 = "error";
            extra2 = e.getMessage();
        }

        // Always send response if payload was valid enough to get clientId
        try {
            String responsePayload = extra1 + ":" + extra2;
            sendMqttMessage("/server/" + clientId + "/register_response", responsePayload);
            System.out.println("Sent registration response to: " + clientId + " status: " + (extra1.equals("error") ? "ERROR" : "SUCCESS"));
            if (!extra1.equals("error")){
                File userCrtFile = new File("user_certs/" + record.email);
                if (!userCrtFile.getParentFile().exists()) {
                    userCrtFile.getParentFile().mkdirs();
                }
                try (FileWriter writer = new FileWriter(userCrtFile)) {
                    writer.write(extra2);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send MQTT response: " + e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleCrtRequest(String topic, String payload){
        try {
            String[] parts = payload.split(":");
            StringBuilder message = new StringBuilder(parts[0]);
            for (int i=1; i<parts.length; i++) {
                String email = parts[i];
                File userCrtFile = new File("user_certs/" + email);
                String crt = "unknown";
                try (InputStream is = new FileInputStream(userCrtFile)){
                    byte[] buf = new byte[is.available()];
                    is.read(buf);
                    crt = new String(buf);
                } catch (Exception e){ System.err.println(e.getMessage()); }
                message.append(":").append(crt);
            }
            topic = "/server" + topic.replace("server/crt_request","crt_response");
            System.out.println("Generated response: topic=\""+topic+"\" message=\""+message+"\"");
            sendMqttMessage(topic, message.toString());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void handleUnregisterRequest(String topic, String payload){
        try {
            boolean success = false;
            File userCrtFile = new File("user_certs/" + payload);
            if (userCrtFile.exists()){
                success = userCrtFile.delete();
            }
            topic = "/server" + topic.replace("server/unregister_request","unregister_response");
            String message = success?"success":"error";
            System.out.println("Generated response: topic=\""+topic+"\" message=\""+message+"\"");
            sendMqttMessage(topic, message);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }


    public static void sendMqttMessage(String topic, String payload) throws Exception {
        if (mqttClient == null || !mqttClient.isConnected()) {
            System.err.println("MqttClient not connected. Cannot send message.");
            return;
        }
        MqttMessage message = new MqttMessage(payload.getBytes(), 1, false, null);
        mqttClient.publish(topic, message);
    }

}
