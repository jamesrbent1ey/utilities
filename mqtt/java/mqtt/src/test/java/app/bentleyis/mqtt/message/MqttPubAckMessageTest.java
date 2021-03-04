/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttPubAckMessageTest {
    public static final int TEST_PACKET_ID =1;

    MqttPubAckMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttPubAckMessage(TEST_PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttPubAckMessage(65536);
            fail("should throw");
        } catch (Exception e) {
            // success
        }
    }

    @Test
    public void getPacketIdentifier() {
        assertEquals(TEST_PACKET_ID, underTest.getPacketIdentifier());
    }

    @Test
    public void toBytes() throws IOException {
        byte[] bytes = underTest.toBytes();
        assertEquals(2, bytes[1]);
    }
}