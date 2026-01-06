package mg.mgmap.activity.mgmap.features.shareloc;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;

import mg.mgmap.generic.util.basic.MGLog;

public class MqttUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static Handler timer = new Handler();

    static void updateCertificate(Context context, SharePerson me, List<SharePerson> persons){

        try (InputStream caCrt = context.getAssets().open("ca.crt");
             InputStream clientCrt = new FileInputStream(new File(context.getFilesDir(), "certs/my.crt"));
             InputStream clientKey = new FileInputStream(new File(context.getFilesDir(), "certs/my.key")) ) {

            MqttBase mqttBase = new MqttBase("CrtRequest",caCrt, clientCrt, clientKey){
                @Override
                protected void messageReceived(String topic, String message) {
                    super.messageReceived(topic, message);
                    if (topic.equals("/server/"+me.email+"/crt_response")){
                        String[] parts = message.split(":");
                        if (getClientId().equals(parts[0]) && (parts.length == persons.size()+1)){
                            for (int i=0; i< persons.size(); i++){
                                SharePerson person = persons.get(i);
                                person.crt = parts[i+1];
                                person.changed();
                            }
                            stop();
                        }
                    }
                }

                @Override
                protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                    if (!reconnect){
                        registerTopic("/server/"+me.email+"/crt_response");
                        StringBuilder message = new StringBuilder(getClientId());
                        for (SharePerson person : persons){
                            message.append(":").append(person.email);
                        }
                        sendMessage("/"+me.email+"/server/crt_request", message.toString());
                    }
                }
            };
            timer.postDelayed(mqttBase::stop, 30000); // kill it after 30s, if
        }catch (Exception e){
            mgLog.e(e);
            for (SharePerson person : persons){
                person.crt = "network error - "+e.getMessage();
            }
        }

    }

    static void unregister(Context context, SharePerson me){
        // me can be changed after return from this message
        String requestTopic = "/"+me.email+"/server/unregister_request";
        String responseTopic = "/server/"+me.email+"/unregister_response";
        String me_email = me.email;

        try (InputStream caCrt = context.getAssets().open("ca.crt");
             InputStream clientCrt = new FileInputStream(new File(context.getFilesDir(), "certs/my.crt"));
             InputStream clientKey = new FileInputStream(new File(context.getFilesDir(), "certs/my.key")) ) {

            MqttBase mqttBase = new MqttBase("UnregisterRequest",caCrt, clientCrt, clientKey){
                @Override
                protected void messageReceived(String topic, String message) {
                    super.messageReceived(topic, message);
                    if (topic.equals(responseTopic)){
                        if ("success".equals(message)){
                            mgLog.i("successful unregister of "+me_email);
                            Toast.makeText(context, "Successfully unregistered on server.", Toast.LENGTH_SHORT).show();
                        }
                        stop();
                    }
                }

                @Override
                protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                    if (!reconnect){
                        registerTopic(responseTopic);
                        sendMessage(requestTopic, me_email);
                    }
                }
            };
            timer.postDelayed(mqttBase::stop, 30000); // kill it after 30s, if
        }catch (Exception e){
            mgLog.e(e);
        }

    }

}
