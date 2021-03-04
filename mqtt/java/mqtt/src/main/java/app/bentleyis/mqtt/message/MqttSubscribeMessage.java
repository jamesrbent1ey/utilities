/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MqttSubscribeMessage extends MqttMessage {

    int packetIdentifier; //2 bytes
    LinkedHashMap<String,Integer> topicFilters = new LinkedHashMap<>();

    public MqttSubscribeMessage(int packetIdentifier) {
        super();
        setControlPacketType(MqttMessageType.SUBSCRIBE.toByte());
        if(packetIdentifier > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetIdentifier);
        }
        this.packetIdentifier = packetIdentifier;
    }

    public void addTopicFilter(String topicName, int qos) {
        topicFilters.put(topicName,qos);
    }

    public void removeTopicFilter(String topicName) {
        topicFilters.remove(topicName);
    }

    public Map<String,Integer> getTopicFilters() {
        return new LinkedHashMap<>(topicFilters);
    }

    public int getPacketId() {
        return packetIdentifier;
    }

    @Override
    public byte[] toBytes() throws IOException {
        if(topicFilters.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one topic filter");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // packet identifier
        payloadBos.write((byte)((packetIdentifier >> 8)&0x00ff));
        payloadBos.write((byte)(packetIdentifier&0x00ff));

        // add all topic filters
        for(String topicName: topicFilters.keySet()) {
            int qos = topicFilters.get(topicName);
            payloadBos.write(encodeString(topicName));
            payloadBos.write((byte)(qos & 0x00ff));
        }

        byte[] payload = payloadBos.toByteArray();
        payloadBos.close();

        // assemble packet
        remainingLength = payload.length;
        bos.write(super.toBytes());
        bos.write(payload);

        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
}
