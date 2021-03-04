/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttDisconnectMessage extends MqttMessage {
    public MqttDisconnectMessage() {
        super();
        setControlPacketType(MqttMessageType.DISCONNECT.toByte());
    }
}
