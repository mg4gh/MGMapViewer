package mg.mgmap.activity.mgmap.features.shareloc;

import android.content.Context;
import android.os.Handler;

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
        new Thread(){
            @Override
            public void run() {
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
        }.start();
    }

}
