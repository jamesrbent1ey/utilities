/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public class MqttPingReqMessage extends MqttMessage {
    public MqttPingReqMessage() {
        super();
        setControlPacketType(MqttMessageType.PINGREQ.toByte());
    }
}
