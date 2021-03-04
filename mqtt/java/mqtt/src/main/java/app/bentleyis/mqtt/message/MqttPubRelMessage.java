/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttPubRelMessage extends MqttPubAckMessage {
    public MqttPubRelMessage(int packetId) {
        super(packetId);
        setControlPacketType(MqttMessageType.PUBREL.toByte());
    }
}
