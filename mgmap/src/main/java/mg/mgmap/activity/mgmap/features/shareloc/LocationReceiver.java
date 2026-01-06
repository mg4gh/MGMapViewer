package mg.mgmap.activity.mgmap.features.shareloc;

import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

public class LocationReceiver {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    ShareLocConfig config;
    MqttBase receiveClient = null;
    SharePerson me;

    public LocationReceiver(MGMapApplication application, FSShareLoc fsShareLoc, SharePerson me, ShareLocConfig config, Map<String, MultiPointModelImpl> shareLocMap) throws  Exception{
        this.me = me;
        this.config = config;
        try (InputStream caCrt = application.getAssets().open("ca.crt");
             InputStream clientCrt = new FileInputStream(new File(application.getFilesDir(), "certs/my.crt"));
             InputStream clientKey = new FileInputStream(new File(application.getFilesDir(), "certs/my.key")) ) {

            receiveClient = new MqttBase("LocationReceiver",caCrt, clientCrt, clientKey ){
                @Override
                protected void messageReceived(String topic, String message) {
                    try {
                        super.messageReceived(topic, message);
                        message = CryptoUtils.decrypt(message, new File(application.getFilesDir(), "certs/my.key"));
                        super.messageReceived(topic, message);

                        String[] parts = topic.split("/");
                        String[] msgParts = message.split("::");
                        PointModel pm = LocationMessage.fromMessage(msgParts[0]);
                        PointModel pmLast = null;
                        if (msgParts.length > 1){
                            pmLast = LocationMessage.fromMessage(msgParts[1]);
                        }
                        mgLog.i("Received pm: "+pm+ " pmLast: "+pmLast);

                        boolean changed = false;
                        for (SharePerson person : config.persons){
                            if (parts[1].equals(person.email) ){
                                MultiPointModelImpl mpmi = new MultiPointModelImpl();
                                shareLocMap.put(person.email, mpmi);
                                if (pmLast != null){
                                    mpmi.addPoint(pmLast);
                                }
                                mpmi.addPoint(pm);
                                fsShareLoc.onLocationReceived(pm);
                                changed = true;
                            }
                        }
                        if (changed){
                            synchronized (shareLocMap){
                                shareLocMap.notifyAll();
                            }
                        }
                    } catch (Exception e) {
                        mgLog.e(e);
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
        receiveClient.stop();
    }


}
