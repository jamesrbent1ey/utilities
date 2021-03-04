/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.util.IllegalFormatException;

/**
 * The MqttMessageFactory allows for parsing blocks of bytes into MqttMessage objects
 */
public class MqttMessageFactory {
    public static final int CLIENT_ID_OFFSET_CONN = 10;
    public static final int CONN_FLAGS_OFFSET = 7;
    public static final int CONN_KEEPALIVE_OFFSET = 8;
    public static final int CONNACK_SESSION_PRESENT_OFFSET = 2;
    public static final int CONNACK_RETURN_CODE_OFFSET = 3;
    public static final int PACKET_ID_MSB_OFFSET = 2;
    public static final int PACKET_ID_LSB_OFFSET = 3;
    public static final int REMAINING_LEN_OFFSET = 1;

    /**
     * Parse the given block of bytes into an MqttMessage
     * @param message message to parse
     * @return MqttMessage, null if presented with null or empty message
     * @Throws IllegalArgumentException for invalid types specified in the message
     */
    public static MqttMessage parseMessage(byte[] message) {
        if(message == null || message.length == 0)
            return null;
        // MqttMessageType.fromByte will throw if not a correct type, no need for defaults
        switch (MqttMessageType.fromByte((byte)((message[0] >> 4) & 0x0f))) {
            case CONNECT:
                return parseConnect(message);
            case CONNACK:
                return parseConnAck(message);
            case PUBLISH:
                return parsePublish(message);
            case PUBACK:
                return parsePubAck(message);
            case PUBREC:
                return parsePubRec(message);
            case PUBREL:
                return parsePubRel(message);
            case PUBCOMP:
                return parsePubComp(message);
            case SUBSCRIBE:
                return parseSubscribe(message);
            case SUBACK:
                return parseSubAck(message);
            case UNSUBSCRIBE:
                return parseUnsubscribe(message);
            case UNSUBACK:
                return parseUnsuback(message);
            case PINGREQ:
                return parsePingReq(message);
            case PINGRESP:
                return parsePingResp(message);
            case DISCONNECT:
                return parseDisconnect(message);
        }
        // this should not be reachable
        return null;
    }

    private static MqttMessage parseDisconnect(byte[] message) {
        // the message already validated type and Disconnect has no payload or var header
        // remaining length is always 1 byte, no payload
        return new MqttDisconnectMessage();
    }

    private static MqttMessage parsePingResp(byte[] message) {
        // the message already validated type and Ping has no payload or var header
        // remaining length is always 1 byte, no payload
        return new MqttPingRespMessage();
    }

    private static MqttMessage parsePingReq(byte[] message) {
        // the message already validated type and Ping has no payload or var header
        // remaining length is always 1 byte, no payload
        return new MqttPingReqMessage();
    }

    private static MqttMessage parseUnsuback(byte[] message) {
        // remaining length is always 2, no payload
        int packetId = (((int)message[PACKET_ID_MSB_OFFSET]<<8) | (message[PACKET_ID_LSB_OFFSET] & 0x00f));
        return new MqttUnsubAckMessage(packetId);
    }

    private static MqttMessage parseUnsubscribe(byte[] message) {
        int[] remainingLenOut = new int[1];
        // the remaining length can be 1 to 4 bytes and is used to calculate the payload size
        int remainingLen = decodeRemainingLength(message, REMAINING_LEN_OFFSET, remainingLenOut);
        int offset =  REMAINING_LEN_OFFSET + remainingLenOut[0];

        int packetId = message[offset++] << 8;
        packetId += message[offset++] & 0x00ff;
        remainingLen -= 2; // deduct the 2 bytes of packet id

        MqttUnsubscribeMessage result = new MqttUnsubscribeMessage(packetId);

        while(remainingLen > 0) {
            String topic = decodeString(message,offset);
            offset += topic.length() + 2; // update offset
            result.addTopicFilter(topic);
            remainingLen -= topic.length() + 2; // deduct the length of the topic + its 2 byte size
        }
        return result;
    }

    private static MqttMessage parseSubAck(byte[] message) {
        int[] remainingLenOut = new int[1];
        // the remaining length can be 1 to 4 bytes and is used to calculate the payload size
        int remainingLen = decodeRemainingLength(message, REMAINING_LEN_OFFSET, remainingLenOut);
        int offset =  REMAINING_LEN_OFFSET + remainingLenOut[0];

        int packetId = message[offset++] << 8;
        packetId += message[offset++] & 0x00ff;
        remainingLen -= 2; // deduct the 2 bytes of packet id

        MqttSubAckMessage result = new MqttSubAckMessage(packetId);
        for(int pos=0;remainingLen>0; pos++, remainingLen--) {
            result.addQosReturnCode(pos, message[offset++]);
        }
        return result;
    }

    private static MqttMessage parseSubscribe(byte[] message) {
        int[] remainingLenOut = new int[1];
        // the remaining length can be 1 to 4 bytes and is used to calculate the payload size
        int remainingLen = decodeRemainingLength(message, REMAINING_LEN_OFFSET, remainingLenOut);
        int offset =  REMAINING_LEN_OFFSET + remainingLenOut[0];

        int packetId = message[offset++] << 8;
        packetId += message[offset++] & 0x00ff;
        remainingLen -= 2; // deduct the 2 bytes of packet id

        MqttSubscribeMessage result = new MqttSubscribeMessage(packetId);

        while(remainingLen > 0) {
            String topic = decodeString(message,offset);
            offset += topic.length() + 2; // update offset
            int qos = message[offset++];
            result.addTopicFilter(topic, qos);
            remainingLen -= topic.length() + 3; // deduct the length of the topic + its 2 byte size + qos
        }
        return result;
    }

    private static MqttMessage parsePubComp(byte[] message) {
        // remaining length is always 1 byte, no payload
        int packetId = (((int)message[PACKET_ID_MSB_OFFSET]<<8) | (message[PACKET_ID_LSB_OFFSET] & 0x00f));
        return new MqttPubCompMessage(packetId);
    }

    private static MqttMessage parsePubRel(byte[] message) {
        // remaining length is always 1 byte, no payload
        int packetId = (((int)message[PACKET_ID_MSB_OFFSET]<<8) | (message[PACKET_ID_LSB_OFFSET] & 0x00f));
        return new MqttPubRelMessage(packetId);
    }

    private static MqttMessage parsePubRec(byte[] message) {
        // remaining length is always 1 byte, no payload
        int packetId = (((int)message[PACKET_ID_MSB_OFFSET]<<8) | (message[PACKET_ID_LSB_OFFSET] & 0x00f));
        return new MqttPubRecMessage(packetId);
    }

    private static MqttMessage parsePubAck(byte[] message) {
        // remaining length is always 1 byte, no payload
        int packetId = (((int)message[PACKET_ID_MSB_OFFSET]<<8) | (message[PACKET_ID_LSB_OFFSET] & 0x00f));
        return new MqttPubAckMessage(packetId);
    }

    private static MqttMessage parsePublish(byte[] message) {
        int[] remainingLenOut = new int[1];
        // the remaining length can be 1 to 4 bytes and is used to calculate the payload size
        int remainingLen = decodeRemainingLength(message, REMAINING_LEN_OFFSET, remainingLenOut);
        int offset =  REMAINING_LEN_OFFSET + remainingLenOut[0];
        String topicName = decodeString(message, offset);
        offset += 2 + topicName.length();
        int packetId = message[offset++] << 8;
        packetId += message[offset++] & 0x00ff;

        MqttPublishMessage result = new MqttPublishMessage(topicName, packetId);

        //flags - dup, qos and retain
        result.setQos( (byte)((message[0] & 0x06)>>1) );
        result.setRetain((message[0] & 0x01) == 1);
        result.setDuplicate((message[0] & 0x08) == 0x08);

        int payloadLen = remainingLen - (offset - (remainingLenOut[0]+REMAINING_LEN_OFFSET));
        if(payloadLen > 0) {
            byte[] payload = new byte[payloadLen];
            System.arraycopy(message, offset, payload, 0, payloadLen);
            result.setPayload(payload);
        }
        return result;
    }

    private static MqttMessage parseConnAck(byte[] message) {
        MqttConnAckMessage msg = new MqttConnAckMessage();

        msg.setSessionPresent((message[CONNACK_SESSION_PRESENT_OFFSET] & 0x01)!=0);
        msg.setReturnCode(message[CONNACK_RETURN_CODE_OFFSET]);

        return msg;
    }

    private static MqttMessage parseConnect(byte[] message) {
        int[] remainingLenOut = new int[1];
        // all variable lengths are encoded, decoding remaining length field to calculate offsets
        decodeRemainingLength(message, REMAINING_LEN_OFFSET, remainingLenOut);
        int baseOffset = 1 + remainingLenOut[0];

        // get the basics
        MqttConnectMessage result = new MqttConnectMessage(
                message[CONN_FLAGS_OFFSET + baseOffset],
                ((int)message[CONN_KEEPALIVE_OFFSET + baseOffset]<<8)|((int)message[CONN_KEEPALIVE_OFFSET + 1 + baseOffset]&0x00ff),
                decodeString(message, CLIENT_ID_OFFSET_CONN + baseOffset)
        );
        // parse additional payload
        int offset = CLIENT_ID_OFFSET_CONN  + baseOffset + 2 + result.getClientId().length();

        // order matters: clientid, will topic, will message, user name then password
        byte flags = result.getConnectFlags();
        if((flags & MqttConnectMessage.FLAG_CONTAINS_WILL) != 0) {
            // will topic and message
            String will = decodeString(message, offset);
            offset += will.length() + 2;
            int willMsgLen = ((int)message[offset++]&0x00ff)<<8;
            willMsgLen += message[offset++] & 0x00ff;
            byte[] willMsg = new byte[willMsgLen];
            System.arraycopy(message,offset,willMsg,0, willMsgLen);
            offset += willMsgLen;
            result.setWill( will, willMsg);
        }

        if((flags & MqttConnectMessage.FLAG_CONTAINS_USERNAME) != 0) {
            // user name
            String uname = decodeString(message, offset);
            result.setUserName(uname);
            offset += uname.length() + 2;
        }

        if((flags & MqttConnectMessage.FLAG_CONTAINS_PASSWORD) != 0) {
            // password
            int pwLen = (int)message[offset++]<<8;
            pwLen += message[offset++] & 0x00ff;
            byte[] pw = new byte[pwLen];
            System.arraycopy(message,offset,pw,0, pwLen);
            result.setPassword(pw);
        }

        return result;
    }

    /**
     * A string is encoded 2 bytes for length followed by utf-8 characters
     * @param message
     * @param offset
     * @return
     */
    private static String decodeString(byte[] message, int offset) {
        int length = ((int)message[offset]<<8)|((int)message[offset+1]&0x00ff);

        return new String(message,offset + 2, length);
    }

    /**
     * Remaining length can consume 1 to 4 bytes.
     * @param message
     * @param offset
     * @param bytesConsumedOut
     * @return
     */
    public static int decodeRemainingLength(byte[] message, int offset, int[] bytesConsumedOut ) {
        int index = 0;
        int multiplier = 1;
        int value = 0;
        byte encodedByte;
        do {
            encodedByte = message[offset + index];
            index++;
            value += (encodedByte & 0x7f) * multiplier;
            multiplier *= 128;
            if(index >= 4) {
                throw new IllegalArgumentException("Remaining Length not properly encoded");
            }
        } while((encodedByte & 0x80) != 0);

        bytesConsumedOut[0] = index;
        return value;
    }

}
