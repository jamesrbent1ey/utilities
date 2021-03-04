/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttPubRelMessageTest {

    @Test
    public void construction() throws IOException {
        MqttPubRelMessage underTest;
        try {
            underTest = new MqttPubRelMessage(65536);
        } catch (Exception e) {
            //success
        }
        underTest = new MqttPubRelMessage(1);
        byte[] bytes = underTest.toBytes();
        assertEquals(MqttMessageType.PUBREL.toByte(), (bytes[0] >> 4)&0x0f);
    }
}