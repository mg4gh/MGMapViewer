package mg.mgmap.activity.mgmap.features.shareloc;

import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class LocationReceiver {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    ShareLocConfig config;
    MqttBase receiveClient = null;
    SharePerson me;

    byte[] baMyCrt;
    byte[] baMyKey;
    byte[] baCaCrt;

    public LocationReceiver(MGMapApplication application, FSShareLoc fsShareLoc, SharePerson me, ShareLocConfig config, Map<String, MultiPointModelImpl> shareLocMap) throws  Exception{
        this.me = me;
        this.config = config;

        baMyCrt = IOUtil.readToByteArray( new File(application.getFilesDir(), "certs/my.crt") );
        baMyKey = IOUtil.readToByteArray( new File(application.getFilesDir(), "certs/my.key") );
        baCaCrt = IOUtil.readToByteArray( application.getAssets().open("ca.crt") );

        receiveClient = new MqttBase("LocationReceiver",baCaCrt, baMyCrt, baMyKey ){
            @Override
            protected void messageReceived(String topic, String message) {
                try {
                    super.messageReceived(topic, message);
                    message = CryptoUtils.decrypt(message, baMyKey);
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
                    mgLog.e(e.getMessage(),e);
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

    void stop(){
        receiveClient.stop();
    }


}
