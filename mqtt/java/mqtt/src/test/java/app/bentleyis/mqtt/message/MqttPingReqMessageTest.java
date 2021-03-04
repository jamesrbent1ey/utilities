/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqttPingReqMessageTest {

    @Test
    public void construction() {
        MqttPingReqMessage underTest = new MqttPingReqMessage();
        assertEquals(MqttMessageType.PINGREQ, underTest.getMessageType());
    }
}