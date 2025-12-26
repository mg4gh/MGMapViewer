package mg.mgmap.mgmain;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class MailUtil {

    public static String sendEmail(String recipient, String uuid) {
        final String username = "mgmapviewer@web.de";
        final String password = "UWWbstwzlL4hWDvuhXGW";

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.web.de");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Registration at MGMapServer");
            message.setText("Hello,\n\nYour registration confirmation code is: " + uuid);

            Transport.send(message);
            System.out.println("Email sent successfully to " + recipient);
            return null;
        } catch (Exception e) {
            System.err.println("Failed to send email to " + recipient);
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static void main(String[] args) {
        sendEmail("mg4gh@web.de", "testxxxx");
    }
}
