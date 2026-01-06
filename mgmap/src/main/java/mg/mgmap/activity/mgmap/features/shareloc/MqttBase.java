package mg.mgmap.activity.mgmap.features.shareloc;

import android.os.Looper;
import android.os.SystemClock;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import mg.mgmap.generic.util.basic.MGLog;

public class MqttBase extends MqttClient{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected static final String SERVER_ADDRESS = "kmt6pewiw8ty93su.myfritz.net";
    protected static final int SERVER_PORT = 9999;

    private boolean stop = false;
    private boolean disconnected = false;

    public MqttBase(String clientPrefix, InputStream caCrt, InputStream clientCrt, InputStream clientKey) throws Exception {
        super("ssl://" + SERVER_ADDRESS + ":" + SERVER_PORT, clientPrefix+"_"+ UUID.randomUUID(), new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setSocketFactory(CryptoUtils.getSocketFactory(caCrt, clientCrt, clientKey));
        options.setCleanStart(true);

        new Thread(() -> { // try automatic reconnect until stop
            while (!stop){
                try {
                    SystemClock.sleep(5000);
                    if (disconnected && !isConnected()){
                        connect(options);
                    }
                } catch (Exception e) {
                    mgLog.e(e.getMessage());
                }
            }
        }).start();


        setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                mgLog.e("DisconnectResponse=" + disconnectResponse);
                disconnected = true;
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                registerThrowable(exception);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                messageReceived(topic, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                mgLog.i("serverURI="+serverURI+", Reconnect=" + reconnect);
                disconnected = false;
                try {
                    MqttBase.this.connectComplete(reconnect, serverURI);
                } catch (MqttException e) {
                    registerThrowable(e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                mgLog.i("reasonCode="+reasonCode+", mqttProperties=" + properties);
            }
        });
        connect(options);
    }
    protected void connectComplete(boolean reconnect, String serverURI) throws MqttException {}

    @Override
    public void connect(MqttConnectionOptions options) throws MqttException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            new Thread(() -> {
                try {
                    connect(options);
                } catch (MqttException e) {
                    registerThrowable(e);
                }
            }).start();
        } else {
            super.connect(options);
        }
    }

    public void sendMessage(String topic, String message) throws MqttException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            new Thread(() -> {
                try {
                    sendMessage(topic, message);
                } catch (MqttException e) {
                    registerThrowable(e);
                }
            }).start();
        } else {
            mgLog.d("Message send. Topic: \"" + topic + "\" Payload: \"" + message+"\"");
            publish(topic, new MqttMessage(message.getBytes(), 1, false, null));
        }
    }

    public void registerTopic(String topic) throws MqttException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            new Thread(() -> {
                try {
                    registerTopic(topic);
                } catch (MqttException e) {
                    registerThrowable(e);
                }
            }).start();
        } else {
            subscribe(topic, 1);
        }
    }

    protected void messageReceived(String topic, String message){
        mgLog.d("Message arrived. Topic: \"" + topic + "\" Payload: \"" + message+"\"");
    }

    protected void registerThrowable(Throwable throwable){
        mgLog.e("Asynchronous error occurred", throwable);
    }

    protected void stop(){
        if (!stop){
            stop = true;
            new Thread(() -> {
                try {
                    if (isConnected() || !disconnected){
                        disconnect(0L);
                    }
                    close();
                } catch (MqttException e) {
                    mgLog.e("Error closing MQTT", e);
                }
            }).start();
        } // else already stopped
    }

}
