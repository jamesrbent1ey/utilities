/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttPubRecMessage extends MqttPubAckMessage{
    public MqttPubRecMessage(int packetId) {
        super(packetId);
        setControlPacketType(MqttMessageType.PUBREC.toByte());
    }
}
