/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class MqttSubAckMessageTest {
    private static final int PACKET_ID = 1;
    MqttSubAckMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttSubAckMessage(PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttSubAckMessage(65536);
            fail();
        } catch (Exception e) {
            // success
        }

        try {
            underTest.toBytes();
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
    public void addQosReturnCode() {
        removeQosReturnCodeAt();
    }

    @Test
    public void removeQosReturnCodeAt() {
        List<Integer> returnCodes = underTest.getQosReturnCodes();
        assertTrue(returnCodes.isEmpty());

        underTest.addQosReturnCode(0, MqttSubAckMessage.RETURN_CODE_FAIL);
        returnCodes = underTest.getQosReturnCodes();
        assertFalse(returnCodes.isEmpty());

        underTest.removeQosReturnCodeAt(0);
        returnCodes = underTest.getQosReturnCodes();
        assertTrue(returnCodes.isEmpty());
    }

    @Test
    public void getQosReturnCodes() {
        removeQosReturnCodeAt();
    }

    @Test
    public void toBytes() throws IOException {
        // exception case handled in construction method above
        underTest.addQosReturnCode(0, MqttSubAckMessage.RETURN_CODE_FAIL);
        byte[] bytes = underTest.toBytes();
        assertEquals(PACKET_ID, bytes[3]);
        assertEquals((byte)MqttSubAckMessage.RETURN_CODE_FAIL, bytes[4]);
    }
}