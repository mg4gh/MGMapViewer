package mg.mgmap.activity.mgmap.features.shareloc;

import android.os.Looper;
import android.util.Log;

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
import java.util.UUID;

public abstract class MqttBase {
    private static final String TAG = "MqttBase";
    protected static final String SERVER_ADDRESS = "kmt6pewiw8ty93su.myfritz.net";
    protected static final int SERVER_PORT = 9999;
    
    protected MqttClient client;
    protected String clientId;

    public MqttBase(InputStream caCrt, InputStream clientCrt, InputStream clientKey) throws Exception {
        String broker = "ssl://" + SERVER_ADDRESS + ":" + SERVER_PORT;
        this.clientId = "AppClient_"+ UUID.randomUUID();
        this.client = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setSocketFactory(CryptoUtils.getSocketFactory(caCrt, clientCrt, clientKey));
        options.setCleanStart(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                Log.e(TAG, "Disconnected: " + disconnectResponse.getReasonString());
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
                Log.i(TAG, "Connect complete. Reconnect: " + reconnect);
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Thread connectThread = new Thread(() -> {
                try {
                    client.connect(options);
                } catch (MqttException e) {
                    registerThrowable(e);
                }
            });
            connectThread.start();
            try {
                // Wait with the current thread (UI thread) until the connect is finished
                connectThread.join();
            } catch (InterruptedException e) {
                registerThrowable(e);
                Thread.currentThread().interrupt();
            }
        } else {
            client.connect(options);
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
            client.publish(topic, new MqttMessage(message.getBytes(), 1, false, null));
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
            client.subscribe(topic, 1);
        }
    }

    protected abstract void messageReceived(String topic, String message);

    protected void registerThrowable(Throwable throwable){
        Log.e(TAG, "Asynchronous error occurred", throwable);
    }

    protected void connectionLost(Throwable cause) {
        registerThrowable(cause);
    }
}
