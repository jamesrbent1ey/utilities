/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;

import app.bentleyis.mqtt.MqttContext;
import app.bentleyis.mqtt.MqttProtocolHandler;
import app.bentleyis.mqtt.message.MqttConnAckMessage;
import app.bentleyis.mqtt.message.MqttMessage;
import app.bentleyis.mqtt.message.MqttMessageFactory;
import app.bentleyis.mqtt.message.MqttPublishMessage;

/**
 * This is only an example. there are several ways to implement a server.
 * this example was designed to handle non-persistent connections. This example assumes that
 * there is one InMemServer per connection. No threading/concurrency is used, transactions are all
 * handled on the connections thread for a pre-defined duration. The protocol is used to recover
 * connection state for continued delivery.
 *
 * In this example, a client connection is provided by an set of streams and a maximum
 * run-time duration, specified in seconds. The server expects a connect transaction from
 * the client. After successful connect, packets will be delivered from all active subscriptions,
 * until all messages are delivered or the maximum duration limit is reached.
 *
 * The client will need to be aware of this activity in order to respect Qos (retransmissions and
 * acknowlegements) as well as keepalive, in order to insure proper message delivery.
 *
 * Unit tests, for this class, are an example of a client.
 */
public class InMemServer {
    MqttProtocolHandler handler;
    OutputStream outputStream;
    InputStream inputStream;
    String clientId;
    String sessionId;
    int maxDurationMs;
    boolean connected;
    static int messageId = 1;
    long startTime;
    Context context;

    // this is just a simple example for managing state. options include persisting state with
    // queued messages.
    LinkedHashMap<Integer,MqttPublishMessage> forAck = new LinkedHashMap<>();
    LinkedHashMap<Integer,MqttPublishMessage> forRec = new LinkedHashMap<>();
    LinkedHashMap<Integer,MqttPublishMessage> forComp = new LinkedHashMap<>();

    public boolean isConnected() {
        return connected;
    }

    /**
     * For the given duration, this server will handle incoming messages (received on in) and
     * produce messages sourced from identified queues (emitted on out).
     * @param in input from the connected client
     * @param out protocol and client message pipe to client
     * @param maxDurationSeconds run time limit
     * @param sessionId used as client id when client id not present
     * @throws IOException on communication error
     */
    public void handleConnection(InputStream in, OutputStream out, int maxDurationSeconds, String sessionId) throws IOException {
        context = new Context();
        startTime = System.currentTimeMillis();
        outputStream = out;
        inputStream = in;
        clientId = sessionId; // start with sessionId for client id unless overwritten in connect
        this.sessionId = sessionId;
        maxDurationMs = maxDurationSeconds * 1000;
        handler = new MqttProtocolHandler();

        while(!done()) {
            // establish a connection or close
            MqttMessage message = handler.handleConnection(context, in, out);

            if(message instanceof MqttConnAckMessage) {
                connected = true;
            }

            if(!connected) {
                throw new IOException("Connect failed");
            }

            // once the connection is established, begin sending packets from any active subscriptions
            sendPackets();

            if(!connected)
                break;
        }
    }

    /**
     * In this example, sending the initial packet will work through the entire queue. This example
     * works with Context.setDelivered, to deliver the next packet in a queue. It is recursive which
     * will impact stack.
     */
    private void sendPackets() throws IOException {
        synchronized (context) {
            List<Queue> queues = Queue.getQueues(clientId);
            for (Queue queue : queues) {
                sendPacket(queue.peek());
            }
        }
    }

    private void sendPacket(byte[] msg) throws IOException {
        synchronized (context) {
            if (msg == null)
                return;

            if (done())
                throw new IOException("times up");

            byte[] res = handler.publish(context, (MqttPublishMessage) MqttMessageFactory.parseMessage(msg));
            outputStream.write(res);
        }
    }

    private boolean done() {
        if((System.currentTimeMillis() - maxDurationMs) > startTime)
            return true; // max run-time exceeded.

        return false;
    }

    /*
     * Context - Encapsulated to hide the public methods from the user
     */
    class Context implements MqttContext {
        @Override
        public void setClientId(String id) {
            clientId = id;
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public void distributeTo(String topic, MqttMessage message) {
            try {
                Queue.dispatch(message.toBytes(), topic);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void holdForAck(int id, MqttPublishMessage message) {
            forAck.put(id, message);
        }

        @Override
        public synchronized MqttPublishMessage getForAck(int id) {
            return forAck.remove(id);
        }

        @Override
        public synchronized void holdForRec(int id, MqttPublishMessage message) {
            forRec.put(id, message);
        }

        @Override
        public synchronized MqttPublishMessage getForRec(int id) {
            return forRec.remove(id);
        }

        @Override
        public synchronized void holdForComp(int id, MqttPublishMessage message) {
            forComp.put(id, message);
        }

        @Override
        public synchronized MqttPublishMessage getForComp(int id) {
            return forComp.remove(id);
        }

        @Override
        public synchronized void setDelivered(int id, MqttPublishMessage message) {
            if (!connected)
                return;

            Queue queue = Queue.getQueue(clientId, message.getTopicName());
            if (queue == null) {
                return;
            }
            // for this simplified example we assume in-order delivery and acknowledgement
            queue.dequeue();

            // now we can send the next queued packet, if present
            try {
                sendPacket(queue.peek());
            } catch (IOException e) {
                connected = false;
                e.printStackTrace();
            }
        }

        @Override
        public synchronized int getMessageId() {
            if (messageId > 65535)
                messageId = 1;
            return messageId++;
        }

        @Override
        public int auth(String uname, byte[] password) {
            // not supported in this example
            return 0;
        }

        @Override
        public String generateClientId() {
            return sessionId;
        }

        @Override
        public void setWill(byte[] message, int qos, boolean retain, String willTopic) {
            // not supported in this example
        }

        @Override
        public int getWillQos() {
            // not supported in this example
            return 0;
        }

        @Override
        public byte[] getWillMessage() {
            // not supported in this example
            return new byte[0];
        }

        @Override
        public boolean retainWill() {
            // not supported in this example
            return false;
        }

        @Override
        public String getWillTopic() {
            // not supported in this example
            return null;
        }

        @Override
        public void addListener(String topic, int qos) {
            Queue.initQueue(clientId, topic, maxDurationMs, qos);
        }

        @Override
        public void removeListener(String topic) {
            Queue.releaseQueue(clientId, topic);
        }

        @Override
        public void ping() {
            // for connection management
            List<Queue> queues = Queue.getQueues(getClientId());
            if(queues == null || queues.isEmpty())
                return;
            for(Queue queue: queues) {
                queue.setLastAccessTime(System.currentTimeMillis());
            }
        }

        @Override
        public void close() {
            // we could clean up queues here but, for this example, queues are discarded
            connected = false;
        }
    }
}
