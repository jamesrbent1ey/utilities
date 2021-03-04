/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import app.bentleyis.mqtt.MqttContext;
import app.bentleyis.mqtt.message.MqttConnAckMessage;
import app.bentleyis.mqtt.message.MqttConnectMessage;
import app.bentleyis.mqtt.message.MqttDisconnectMessage;
import app.bentleyis.mqtt.message.MqttMessage;
import app.bentleyis.mqtt.message.MqttMessageFactory;
import app.bentleyis.mqtt.message.MqttMessageType;
import app.bentleyis.mqtt.message.MqttPingReqMessage;
import app.bentleyis.mqtt.message.MqttPingRespMessage;
import app.bentleyis.mqtt.message.MqttPubAckMessage;
import app.bentleyis.mqtt.message.MqttPubCompMessage;
import app.bentleyis.mqtt.message.MqttPubRecMessage;
import app.bentleyis.mqtt.message.MqttPubRelMessage;
import app.bentleyis.mqtt.message.MqttPublishMessage;
import app.bentleyis.mqtt.message.MqttSubAckMessage;
import app.bentleyis.mqtt.message.MqttSubscribeMessage;
import app.bentleyis.mqtt.message.MqttUnsubAckMessage;
import app.bentleyis.mqtt.message.MqttUnsubscribeMessage;

/**
 * MQTT Protocol Handler.
 * This class can handle a stream based connection (handleConnection) or operate on the packet level
 * via publish and receive methods. The handleConnection method uses the receive method.
 *
 * An MqttContext object is required on all transactions. the MqttContext is responsible for managing
 * connections, concurrency and distribution. The protocol handler also utilizes the MqttContext to
 * help manage message state.
 */
public class MqttProtocolHandler {

    /**
     * Caller may execute this call in a loop/thread This will handle a single transaction. This is
     * the stream-based entry point
     * @param context
     * @param in
     * @param out
     * @return MqttMessage null if no response to transaction. Any response will have already been sent via out
     * @throws IOException on error
     */
    public MqttMessage handleConnection(MqttContext context, InputStream in, OutputStream out)
            throws IOException {
        byte b = (byte) in.read();
        if(b == -1) {
            // stream closed
            throw new IOException("Input Closed");
        }
        try {
            MqttMessageType type = MqttMessageType.fromByte((byte) ((b >> 4) & 0x0f));
        } catch (IllegalArgumentException e) {
            // reset the connection
            throw new IOException("Out of sync, reset");
        }
        // looks good so far
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(b);
        int cnt = 0;
        do {
            b = (byte)in.read();
            bos.write(b);
            cnt++;
        } while( cnt<4 && (b&0x80)!=0);

        // now we should have remaining length
        byte[] bytes = bos.toByteArray();
        int[] consumed = new int[1];
        int remainingLength = 0;
        try {
            remainingLength = MqttMessageFactory.decodeRemainingLength(bytes, 1, consumed);
        } catch (IllegalArgumentException e) {
            // reset the connection
            throw new IOException("bad length, reset");
        }
        bytes = new byte[remainingLength];
        in.read(bytes);
        bos.write(bytes);
        bytes = bos.toByteArray();
        bos.close();

        MqttMessage response = receive(context, bytes);
        if(response != null) {
            out.write(response.toBytes());
        }
        return response;
    }

    /**
     * This is called to publish Message - Packet Based. Note that the packet id is replaced by
     * a context generated id unless the packet is marked duplicate (i.e. it is being re-sent).
     * The return should be written to the output of the connection. The context is called to enable
     * suitable packet tracking.
     * @param context context used to manage packet retention as necessary.
     * @return the message, properly formatted for delivery to the client.
     * @throws IOException
     */
    public byte[] publish(MqttContext context, MqttPublishMessage pub) throws IOException {
        if(!pub.isDuplicate())
            pub.setPacketIdentifier(context.getMessageId());
        byte[] result = pub.toBytes();

        // we set the duplicate flag, after converting to bytes, in the event that a retransmission
        // is necessary.
        pub.setDuplicate(true);

        // Based on Qos, we may ask teh Context to hold the packet
        switch(pub.getQos()) {
            case MqttPublishMessage.QOS_AT_LEAST_ONCE:
                context.holdForAck(pub.getPacketId(), pub);
                break;
            case MqttPublishMessage.QOS_EXACTLY_ONCE:
                context.holdForRec(pub.getPacketId(), pub);
                break;
            case MqttPublishMessage.QOS_AT_MOST_ONCE:
            default:
                context.setDelivered(pub.getPacketId(), pub);
                break; // don't expect an ack
        }

        return result;
    }

    /**
     * Receive a message for the given context. This is the packet-based entry point. Use the toBytes
     * method of the returned value, if not null, to send data back to the source of message.
     * @param context
     * @param message
     * @return
     * @throws IOException
     */
    public MqttMessage receive(MqttContext context, byte[] message) throws IOException {
        if(message == null || message.length == 0) {
            throw new IOException("empty message");
        }

        try {
            return handleMessage(context, MqttMessageFactory.parseMessage(message));
        } catch (Exception e) {
            throw new IOException("Failed to parse message");
        }
    }

    /**
     * Handle a parsed message
     * @param context context for the call
     * @param message message to handle
     * @return response, null if no response
     */
    private MqttMessage handleMessage(MqttContext context, MqttMessage message) throws IOException {
        // only called by receive which guarantees message to not be null
        switch(message.getMessageType()) {
            case CONNECT:
                return handleConnectMessage(context, (MqttConnectMessage) message);
            case PUBLISH:
                return handlePublish(context, (MqttPublishMessage) message);
            case PUBACK:
                return handlePubAck(context, (MqttPubAckMessage) message);
            case PUBREL:
                return handlePubRel(context, (MqttPubRelMessage) message);
            case PUBREC:
                return handlePubRec(context, (MqttPubRecMessage) message);
            case PUBCOMP:
                return handlePubComp(context, (MqttPubCompMessage) message);
            case PINGREQ:
                return handlePingReq(context, (MqttPingReqMessage) message);
            case SUBSCRIBE:
                return handleSubscribe(context, (MqttSubscribeMessage) message);
            case UNSUBSCRIBE:
                return handleUnsubscribe(context, (MqttUnsubscribeMessage) message);
            case DISCONNECT:
                return handleDisconnect(context, (MqttDisconnectMessage) message);
        }
        return null;
    }

    /**
     * Handle publish acknowlegement. This is received by the publisher
     * @param context context for the call
     * @param message puback message received
     * @return null, no further processing required
     */
    private MqttMessage handlePubAck(MqttContext context, MqttPubAckMessage message) {
        // this completes it.
        int id = message.getPacketIdentifier();
        context.setDelivered(id, context.getForAck(id));
        return null;
    }

    /**
     * Handle a request to unsubscribe to a topic
     * @param context context for the call
     * @param message unsubscribe message
     * @return unsubscription ack message
     */
    private MqttMessage handleUnsubscribe(MqttContext context, MqttUnsubscribeMessage message) {
        MqttUnsubAckMessage response = new MqttUnsubAckMessage(message.getPacketIdentifier());
        List<String> topics = message.getTopicFilters();
        for(String topic: topics) {
            context.removeListener(topic);
        }
        return response;
    }

    /**
     * Handle a request to subscribe to one or more topics
     * @param context context for the call
     * @param message subscription request
     * @return subscription acknowledgement message.
     */
    private MqttMessage handleSubscribe(MqttContext context, MqttSubscribeMessage message) {
        // context is the session. The context handles queuing and queue management
        MqttSubAckMessage response = new MqttSubAckMessage(message.getPacketId());
        Map<String,Integer> filters = message.getTopicFilters();
        int pos = 0;
        for(String filter : filters.keySet()) {
            // add listeners, for the given context, to each topic specified
            context.addListener(filter, filters.get(filter));
            // TODO this must be in the order presented in the message Map can't guarantee this.
            response.addQosReturnCode(pos++, filters.get(filter));
        }
        return response;
    }

    /**
     * A publish release message is the 2nd message for Qos2. The publish should've been stored by
     * the context, reliably. As such we notify the publisher that the handshake is complete.
     * @param context context for the call
     * @param message publish release message
     * @return publish complete message
     */
    private MqttMessage handlePubRel(MqttContext context, MqttPubRelMessage message) {
        int id = message.getPacketIdentifier();
        return new MqttPubCompMessage(id);
    }

    /**
     * Handle a publish message. Context must reliably handle (persist/deliver) the message.
     * @param context context for the call
     * @param message publish message
     * @return pub ack for Qos1, PubRec for Qos2, otherwise null
     */
    private MqttMessage handlePublish(MqttContext context, MqttPublishMessage message) {
        // distribute the packet to subscribers
        context.distributeTo(message.getTopicName(), message);

        // generate response dependent on qos
        MqttPubAckMessage response = null;
        switch(message.getQos()) {
            case MqttPublishMessage.QOS_AT_LEAST_ONCE:
                response = new MqttPubAckMessage(message.getPacketId());
                break;
            case MqttPublishMessage.QOS_EXACTLY_ONCE:
                response = new MqttPubRecMessage(message.getPacketId());
                break;
            case MqttPublishMessage.QOS_AT_MOST_ONCE: // fall through to default
            default:
                break; // no need to ack
        }

        return response;
    }

    /**
     * Handle disconnection
     * @param context context for the call
     * @param message disconnect message
     * @return null, no further action
     */
    private MqttMessage handleDisconnect(MqttContext context, MqttDisconnectMessage message) {
        // the context is the session - this allows context to clean up and terminate
        context.close();
        // no ack
        return null;
    }

    /**
     * Respond to a Ping request with a ping response
     * @param context context for the call
     * @param message request message
     * @return ping response message
     */
    private MqttMessage handlePingReq(MqttContext context, MqttPingReqMessage message) {
        context.ping();
        return new MqttPingRespMessage();
    }

    /**
     * Handle a Connect message, called from handleMessage - no additional parameter checking
     * @param context conext for the call
     * @param message connect message
     * @return MqttConnAckMessage
     */
    private MqttMessage handleConnectMessage(MqttContext context, MqttConnectMessage message) throws IOException {
        byte[] pw = message.getPassword();
        String uname = message.getUserName();
        MqttConnAckMessage ackMessage = new MqttConnAckMessage();
        String clientId = message.getClientId();
        boolean cleanSession = message.cleanSession();

        // TODO server unavailable?

        // TODO protocol version and name checking

        // refactor
        if(clientId == null || clientId.length() == 0){
            if(!cleanSession) {
                ackMessage.setReturnCode((byte) MqttConnAckMessage.RETURN_CODE_BAD_ID);
                return ackMessage;
            }
            // assign client id to establish connection
            clientId = context.generateClientId();
        }

        // handle auth
        if(pw != null) {
            switch( context.auth(uname, pw) ) {
                case MqttContext.AUTH_INVALID_CREDENTIALS:
                    ackMessage.setReturnCode((byte) MqttConnAckMessage.RETURN_CODE_BAD_CREDENTIAL);
                    return ackMessage;
                case MqttContext.AUTH_NOT_AUTHORIZED:
                    ackMessage.setReturnCode((byte) MqttConnAckMessage.RETURN_CODE_UNAUTHORIZED);
                    return ackMessage;
            }
        }

        // handle session presence after auth checks to not expose state
        if( context.getClientId() != null ) {
            ackMessage.setSessionPresent(true);
        }
        context.setClientId(clientId);

        // store will for future processing
        context.setWill(message.getWillMessage(), message.getWillQos(), message.retainWill(),
                message.getWillTopic());

        return ackMessage;
    }

    /**
     * Publish Received is the first ack for Qos2 delivery of a Publish message. This is received
     * by the publisher.
     * @param context context for the call
     * @param message the publish release message received
     * @return publish release message to be returned
     */
    private MqttMessage handlePubRec(MqttContext context, MqttPubRecMessage message) {
        // received initial ack/received - change state to wait for complete, send release
        int id = message.getPacketIdentifier();
        context.holdForComp(id, context.getForRec(id));
        return new MqttPubRelMessage(message.getPacketIdentifier());
    }

    /**
     * Publish Complete is the final ack for Qos2 delivery of a Publish message. This is received
     * by the publisher
     * @param context context for the call
     * @param message the publish complete messasge
     * @return null, no further communication required.
     */
    private MqttMessage handlePubComp(MqttContext context, MqttPubCompMessage message) {
        // this is the final Ack for Qos2, complete
        int id = message.getPacketIdentifier();
        context.setDelivered(id, context.getForComp(id));
        return null; // no additional communication
    }

}
