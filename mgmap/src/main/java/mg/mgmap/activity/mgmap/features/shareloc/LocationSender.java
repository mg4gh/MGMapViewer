package mg.mgmap.activity.mgmap.features.shareloc;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;

public class LocationSender implements Observer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final MGMapApplication application;
    private ShareLocConfig config;

    private boolean stop = false;
    final ArrayList<PointModel> point2send = new ArrayList<>();
    MqttBase sendClient = null;
    SharePerson me;
    PointModel lastSentPM = null;

    public LocationSender(MGMapApplication application, SharePerson me, ShareLocConfig shareLocConfig){
        this.application = application;
        this.me = me;
        this.config = shareLocConfig;
        application.lastPositionsObservable.addObserver(this);
        new Thread(() -> {
            while (!stop){
                try {
                    PointModel pm = null;
                    synchronized (point2send){
                        if (!point2send.isEmpty()){
                            pm = point2send.remove(0);
                        } else {
                            point2send.wait(5000);
                        }
                    }
                    if (pm != null){
                        long now = System.currentTimeMillis();

                        for (SharePerson person : config.persons){
                            if (person.shareWithActive && (person.shareWithUntil > now)){
                                sendLocation(person, pm, now);
                            }
                        }
                    }
                } catch (Exception e){
                    mgLog.e(e);
                }
            }
        }).start();
    }

    void stop(){
        application.lastPositionsObservable.deleteObserver(this);
        stop = true;
        if (sendClient != null){
            sendClient.stop();
        }
    }

    void setConfig(ShareLocConfig config){
        this.config = config;
    }

    private void sendLocation(SharePerson person, PointModel pm, long now) throws Exception{
        if (sendClient == null){
            try (InputStream caCrt = application.getAssets().open("ca.crt");
                 InputStream clientCrt = new FileInputStream(new File(application.getFilesDir(), "certs/my.crt"));
                 InputStream clientKey = new FileInputStream(new File(application.getFilesDir(), "certs/my.key")) ) {
                sendClient = new MqttBase("LocationSender", caCrt, clientCrt, clientKey){
                    @Override
                    protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {
                        super.connectComplete(reconnect, serverURI);
                        synchronized (point2send){
                            point2send.notifyAll();
                        }
                    }
                };
            }
        }
        if ((sendClient != null) && (me != null)){
            if (sendClient.isConnected()){
                MqttProperties props = new MqttProperties();
                props.setMessageExpiryInterval(8 * 60 * 60L);
//                props.setMessageExpiryInterval( 600L); // used for test - to see whether msg expires after 10min
                String topic = "/"+me.email+"/"+person.email+"/location";
                String msg = LocationMessage.toMessage(pm) + ((lastSentPM!=null)?("::"+LocationMessage.toMessage(lastSentPM)):"");
                mgLog.d("Message send. (1) Topic: \"" + topic + "\" Payload: \"" + msg+"\"");
                msg = CryptoUtils.encrypt(msg, person);
                mgLog.d("Message send. (2) Topic: \"" + topic + "\" Payload: \"" + msg+"\"");
                sendClient.publish(topic, new MqttMessage(msg.getBytes(), 1, true, props));
                lastSentPM = pm;
            } else {
                if (pm.getTimestamp() + 30 * 60 * 1000 > now){ // pm is less than 30min old
                    synchronized (point2send){
                        point2send.add(0,pm); // put it back to queue
                    }
                }
            }
        }
    }


    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        PointModel pm = application.lastPositionsObservable.lastGpsPoint;
        synchronized (point2send){
            point2send.add(pm);
            point2send.notifyAll();
        }
    }


}
