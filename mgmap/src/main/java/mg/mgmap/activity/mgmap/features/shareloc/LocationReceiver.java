package mg.mgmap.activity.mgmap.features.shareloc;

import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

public class LocationReceiver {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    ShareLocConfig config;
    MqttBase receiveClient = null;
    SharePerson me;

    public LocationReceiver(MGMapApplication application, SharePerson me, ShareLocConfig config, Map<String, MultiPointModelImpl> shareLocMap) throws  Exception{
        this.me = me;
        this.config = config;
        try (InputStream caCrt = application.getAssets().open("ca.crt");
             InputStream clientCrt = new FileInputStream(new File(application.getFilesDir(), "certs/my.crt"));
             InputStream clientKey = new FileInputStream(new File(application.getFilesDir(), "certs/my.key")) ) {

            receiveClient = new MqttBase("LocationReceiver",caCrt, clientCrt, clientKey ){
                @Override
                protected void messageReceived(String topic, String message) {
                    super.messageReceived(topic, message);

                    String[] parts = topic.split("/");

                    PointModel pm = LocationMessage.fromMessage(message);
                    mgLog.i("Received pm: "+pm.toString());

                    boolean changed = false;
                    for (SharePerson person : config.persons){
                        if (parts[1].equals(person.email) ){
                            MultiPointModelImpl mpmi = shareLocMap.get(person.email);
                            if (mpmi == null){
                                mpmi = new MultiPointModelImpl();
                                shareLocMap.put(person.email, mpmi);
                            }
                            mpmi.addPoint(pm);
                            changed = true;
                        }
                    }
                    if (changed){
                        synchronized (shareLocMap){
                            shareLocMap.notifyAll();
                        }
                    }
                }

                @Override
                protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                    super.connectComplete(reconnect, serverURI);
                    if (!reconnect){
                        receiveClient.registerTopic("/+/"+me.email+"/location");
                    }
                }
            };

        }

    }

    void stop(){
//        stop = true;
        receiveClient.stop();
    }


}
