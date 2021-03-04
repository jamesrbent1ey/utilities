/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttPubCompMessageTest {

    @Test
    public void construction() throws IOException {
        MqttPubCompMessage underTest;
        try {
            underTest = new MqttPubCompMessage(65536);
        } catch (Exception e) {
            //success
        }
        underTest = new MqttPubCompMessage(1);
        byte[] bytes = underTest.toBytes();
        assertEquals(MqttMessageType.PUBCOMP.toByte(), (bytes[0] >> 4)&0x0f);
    }
}