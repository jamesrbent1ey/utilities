/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttConnAckMessageTest {
    MqttConnAckMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttConnAckMessage();
    }

    @Test
    public void isSessionPresent() {
        assertFalse(underTest.isSessionPresent());

        underTest.setSessionPresent(true);
        assertTrue(underTest.isSessionPresent());
    }

    @Test
    public void setSessionPresent() {
        isSessionPresent();
    }

    @Test
    public void getReturnCode() {
        assertEquals(MqttConnAckMessage.RETURN_CODE_ACCEPTED, underTest.getReturnCode());

        underTest.setReturnCode((byte) MqttConnAckMessage.RETURN_CODE_BAD_CREDENTIAL);
        assertEquals( MqttConnAckMessage.RETURN_CODE_BAD_CREDENTIAL, underTest.getReturnCode());
    }

    @Test
    public void setReturnCode() {
        getReturnCode();
    }

    @Test
    public void toBytes() throws IOException {
        byte[] bytes = underTest.toBytes();
        assertEquals(0, bytes[3]);

        underTest.setSessionPresent(true);
        underTest.setReturnCode((byte) MqttConnAckMessage.RETURN_CODE_BAD_ID);
        bytes = underTest.toBytes();
        assertEquals(1, bytes[2]);
        assertEquals(MqttConnAckMessage.RETURN_CODE_BAD_ID, bytes[3]);

    }
}