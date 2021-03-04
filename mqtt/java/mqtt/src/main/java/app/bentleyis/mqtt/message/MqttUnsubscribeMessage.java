/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MqttUnsubscribeMessage extends MqttMessage {

    int packetIdentifier;
    LinkedList<String> topicFilters = new LinkedList<>();

    public MqttUnsubscribeMessage(int packetIdentifier) {
        super();
        if(packetIdentifier > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetIdentifier);
        }
        setControlPacketType(MqttMessageType.UNSUBSCRIBE.toByte());
        this.packetIdentifier = packetIdentifier;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public synchronized void addTopicFilter(String topicName) {
        if(topicFilters.contains(topicName)) {
            return;
        }
        topicFilters.add(topicName);
    }

    public void removeTopicFilter(String topicName) {
        topicFilters.remove(topicName);
    }

    public List<String> getTopicFilters() {
        return new LinkedList<>(topicFilters);
    }

    @Override
    public byte[] toBytes() throws IOException {
        if(topicFilters.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one topic");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // packet identifier
        payloadBos.write((byte)((packetIdentifier >> 8)&0x00ff));
        payloadBos.write((byte)(packetIdentifier&0x00ff));

        // add topics
        for(String topic: topicFilters) {
            payloadBos.write(encodeString(topic));
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
