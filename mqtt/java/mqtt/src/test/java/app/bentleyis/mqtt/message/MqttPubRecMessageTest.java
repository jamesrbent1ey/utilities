/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttPubRecMessageTest {

    @Test
    public void construction() throws IOException {
        MqttPubRecMessage underTest;
        try {
            underTest = new MqttPubRecMessage(65536);
        } catch (Exception e) {
            //success
        }
        underTest = new MqttPubRecMessage(1);
        byte[] bytes = underTest.toBytes();
        assertEquals(MqttMessageType.PUBREC.toByte(), (bytes[0] >> 4)&0x0f);
    }
}