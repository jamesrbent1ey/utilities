/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MqttPublishMessage extends MqttMessage {

    public static final int QOS_AT_MOST_ONCE = 0;
    public static final int QOS_AT_LEAST_ONCE = 1;
    public static final int QOS_EXACTLY_ONCE = 2;

    boolean duplicate;
    byte qos;
    boolean retain;
    String topicName;
    int packetIdentifier; //2 bytes
    byte[] payload;

    public MqttPublishMessage(String topicName, int packetIdentifier) {
        super();
        setControlPacketType(MqttMessageType.PUBLISH.toByte());
        if(packetIdentifier > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetIdentifier);
        }
        this.topicName = topicName;
        this.packetIdentifier = packetIdentifier;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public byte getQos() {
        return qos;
    }

    public void setQos(byte qos) {
        if(qos > 2) {
            throw new IllegalArgumentException("Illegal value: "+qos);
        }
        this.qos = qos;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public int getPacketId() {
        return packetIdentifier;
    }
    public void setPacketIdentifier(int id) {
        packetIdentifier = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload; // this risks modification as it's just a reference
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // flags
        byte flags = 0;
        if(retain)
            flags |= 0x01;
        if(duplicate)
            flags |= 0x08;
        flags |= qos << 1;
        super.setFlags(flags);

        //variable header and payload
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // topic name
        payloadBos.write(encodeString(topicName));

        // packet identifier
        payloadBos.write((byte)((packetIdentifier >> 8)&0x00ff));
        payloadBos.write((byte)(packetIdentifier&0x00ff));

        if(this.payload != null) {
            payloadBos.write(this.payload);
        }

        byte[] payload = payloadBos.toByteArray();
        payloadBos.close();

        // assemble the message
        remainingLength = payload.length;
        bos.write(super.toBytes());
        bos.write(payload);
        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
}
