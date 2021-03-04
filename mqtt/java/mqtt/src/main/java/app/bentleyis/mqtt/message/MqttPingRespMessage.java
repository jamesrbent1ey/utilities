/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttPingRespMessage extends MqttMessage {
    public MqttPingRespMessage() {
        super();
        setControlPacketType(MqttMessageType.PINGRESP.toByte());
    }
}
