/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttUnsubAckMessageTest {
    private static final int PACKET_ID = 1;

    MqttUnsubAckMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttUnsubAckMessage(PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttUnsubAckMessage(65536);
            fail();
        } catch (Exception e) {
            // success
        }
    }

    @Test
    public void getPacketIdentifier() {
        assertEquals(PACKET_ID, underTest.getPacketIdentifier());
    }

    @Test
    public void toBytes() throws IOException {
        byte[] bytes = underTest.toBytes();
        assertEquals(PACKET_ID,bytes[3]);
    }
}