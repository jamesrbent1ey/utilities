/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttPublishMessageTest {
    private static final String TEST_TOPIC = "testtopic";
    private static final int TEST_PACKET_ID = 1;
    private static final String TEST_PAYLOAD = "testpayload";

    MqttPublishMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttPublishMessage(TEST_TOPIC, TEST_PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttPublishMessage(TEST_TOPIC, 65536);
            fail("should throw");
        } catch (Exception e) {
            //success
        }
    }

    @Test
    public void isDuplicate() {
        assertFalse(underTest.isDuplicate());

        underTest.setDuplicate(true);
        assertTrue(underTest.isDuplicate());
    }

    @Test
    public void setDuplicate() {
        isDuplicate();
    }

    @Test
    public void getQos() {
        assertEquals(MqttPublishMessage.QOS_AT_MOST_ONCE, underTest.getQos());
        underTest.setQos((byte)MqttPublishMessage.QOS_EXACTLY_ONCE);
        assertEquals(MqttPublishMessage.QOS_EXACTLY_ONCE, underTest.getQos());
        try {
            underTest.setQos((byte) 5);
            fail();
        } catch (Exception e) {
            // success
        }
    }

    @Test
    public void setQos() {
        getQos();
    }

    @Test
    public void isRetain() {
        assertFalse(underTest.isRetain());
        underTest.setRetain(true);
        assertTrue(underTest.isRetain());
    }

    @Test
    public void setRetain() {
        isRetain();
    }

    @Test
    public void getPacketId() {
        assertEquals(TEST_PACKET_ID, underTest.getPacketId());
    }

    @Test
    public void setPacketIdentifier() {
        assertEquals(TEST_PACKET_ID, underTest.getPacketId());
        underTest.setPacketIdentifier(20);
        assertEquals(20, underTest.getPacketId());
    }

    @Test
    public void getTopicName() {
        assertTrue(TEST_TOPIC.equals(underTest.getTopicName()));
    }

    @Test
    public void setPayload() {
        assertNull(underTest.getPayload());
        underTest.setPayload(TEST_PAYLOAD.getBytes());
        byte[] payload = underTest.getPayload();
        assertNotNull(payload);
        assertTrue(TEST_PAYLOAD.equals(new String(payload)));
    }

    @Test
    public void getPayload() {
        setPayload();
    }

    @Test
    public void toBytes() throws IOException {
        byte[] bytes = underTest.toBytes();
        assertTrue((bytes[0] & 0x0f) == 0);

        underTest.setRetain(true);
        bytes = underTest.toBytes();
        assertTrue((bytes[0] & 0x0f) == 1);

        underTest.setDuplicate(true);
        bytes = underTest.toBytes();
        assertTrue((bytes[0] & 0x0f) == 9);

        underTest.setQos((byte)MqttPublishMessage.QOS_AT_LEAST_ONCE);
        bytes = underTest.toBytes();
        assertTrue((bytes[0] & 0x0f) == 11);

        int expectedLength = (2+TEST_TOPIC.length()) + 2; //encoded topic name plus 2 byte packet id
        assertEquals(expectedLength,bytes[1]);

        underTest.setPayload(TEST_PAYLOAD.getBytes());
        bytes = underTest.toBytes();
        expectedLength += TEST_PAYLOAD.length();
        assertEquals(expectedLength,bytes[1]);
    }
}