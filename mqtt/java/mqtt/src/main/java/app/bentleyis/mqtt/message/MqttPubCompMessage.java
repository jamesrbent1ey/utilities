/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttPubCompMessage extends MqttPubAckMessage {
    public MqttPubCompMessage(int packetId) {
        super(packetId);
        setControlPacketType(MqttMessageType.PUBCOMP.toByte());
    }
}
