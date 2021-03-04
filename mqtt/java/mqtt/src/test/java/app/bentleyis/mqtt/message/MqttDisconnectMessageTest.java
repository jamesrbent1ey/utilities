/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqttDisconnectMessageTest {

    @Test
    public void construction() {
        MqttDisconnectMessage underTest = new MqttDisconnectMessage();
        assertEquals(MqttMessageType.DISCONNECT, underTest.getMessageType());
    }
}