/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqttPingRespMessageTest {

    @Test
    public void construction() {
        MqttPingRespMessage underTest = new MqttPingRespMessage();
        assertEquals(MqttMessageType.PINGRESP, underTest.getMessageType());
    }
}