/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttConnectMessageTest {
    private static final String CLIENT_ID = "testclientid";
    private static final String USERNAME = "testusername";
    private static final String PASSWORD = "testpassword";
    private static final String WILLTOPIC = "testtopic";
    private static final String WILLMESSAGE = "testwillmessage";

    MqttConnectMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttConnectMessage((byte)0,30, CLIENT_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttConnectMessage((byte)0,30, null);
            fail();
        }catch (Exception e) {
            //success
        }

        try {
            underTest = new MqttConnectMessage((byte)0,30, "");
            fail();
        }catch (Exception e) {
            //success
        }
    }

    @Test
    public void getConnectFlags() {
        assertEquals((byte)0, underTest.getConnectFlags());
        underTest = new MqttConnectMessage((byte)0xfe,30, CLIENT_ID);
        assertEquals((byte)0xfe, underTest.getConnectFlags());
    }

    @Test
    public void getUserName() {
        underTest.setUserName(USERNAME);
        assertTrue(USERNAME.equals(underTest.getUserName()));

        underTest.setUserName(null);
        assertNull(underTest.getUserName());
    }

    @Test
    public void setUserName() {
        getUserName();
    }

    @Test
    public void getPassword() {
        underTest.setPassword(PASSWORD.getBytes());
        byte[] pw = underTest.getPassword();
        assertNotNull(pw);
        assertTrue(PASSWORD.equals(new String(pw)));

        underTest.setPassword(null);
        assertNull(underTest.getPassword());
    }

    @Test
    public void setPassword() {
        getPassword();
    }

    @Test
    public void getWillTopic() {
        setWill();
    }

    @Test
    public void getWillMessage() {
        setWill();
    }

    @Test
    public void setWill() {
        underTest.setWill(WILLTOPIC,WILLMESSAGE.getBytes());
        assertTrue(WILLTOPIC.equals(underTest.getWillTopic()));
        byte[] tm = underTest.getWillMessage();
        assertNotNull(tm);
        assertTrue(WILLMESSAGE.equals(new String(tm)));
        assertTrue((underTest.connectFlags & MqttConnectMessage.FLAG_CONTAINS_WILL) ==
                MqttConnectMessage.FLAG_CONTAINS_WILL);

        underTest.setWill(null, WILLMESSAGE.getBytes());
        assertTrue((underTest.connectFlags & MqttConnectMessage.FLAG_CONTAINS_WILL) == 0);
        assertNull(underTest.getWillTopic());
        assertNotNull(underTest.getWillMessage());
        underTest.setWill(null,null);
        assertNull(underTest.getWillTopic());
        assertNull(underTest.getWillMessage());
        assertTrue((underTest.connectFlags & MqttConnectMessage.FLAG_CONTAINS_WILL) == 0);
    }

    @Test
    public void setRetainWill() throws IOException {
        underTest.setWill(WILLTOPIC,WILLMESSAGE.getBytes());
        byte[] bytes = underTest.toBytes();
        assertTrue((bytes[9] & MqttConnectMessage.FLAG_RETAIN_WILL) == (byte)0);

        underTest.setRetainWill(true);
        bytes = underTest.toBytes();
        assertTrue((bytes[9] & MqttConnectMessage.FLAG_RETAIN_WILL) == (byte)MqttConnectMessage.FLAG_RETAIN_WILL);
        underTest.setRetainWill(false);
        bytes = underTest.toBytes();
        assertTrue((bytes[9] & MqttConnectMessage.FLAG_RETAIN_WILL) == 0);
    }

    @Test
    public void retainWill() {
        assertFalse(underTest.retainWill());
        underTest.setRetainWill(true);
        assertTrue(underTest.retainWill());
    }

    @Test
    public void getClientId() {
        assertTrue(CLIENT_ID.equals(underTest.getClientId()));
    }

    @Test
    public void setCleanSession() throws IOException {
        underTest.setCleanSession(true);
        byte[] bytes = underTest.toBytes();
        assertTrue(bytes[9] == MqttConnectMessage.FLAG_CLEAN_SESSION);

        underTest.setCleanSession(false);
        bytes = underTest.toBytes();
        assertTrue(bytes[9] == 0);
    }

    @Test
    public void cleanSession() {
        assertFalse(underTest.cleanSession());
        underTest.setCleanSession(true);
        assertTrue(underTest.cleanSession());
    }

    @Test
    public void setWillQos() {
        byte qos = underTest.getWillQos();
        assertEquals(0, underTest.getWillQos());

        underTest.setWillQos((byte)3);
        assertEquals(3, underTest.getWillQos());

        underTest.setWillQos((byte)4);
        assertEquals(0, underTest.getWillQos());
    }

    @Test
    public void getWillQos() {
        setWillQos();
    }

    @Test
    public void toBytes() throws IOException {
        byte[] bytes = underTest.toBytes();
        String segment = new String(bytes, 4,4 );
        assertTrue(segment.equals("MQTT"));

        // TODO should evaluate more header values and computed lengths
        underTest.setPassword(PASSWORD.getBytes());
        underTest.setUserName(USERNAME);
        assertTrue((underTest.connectFlags & MqttConnectMessage.FLAG_CONTAINS_PASSWORD) ==
                MqttConnectMessage.FLAG_CONTAINS_PASSWORD);
        assertTrue((underTest.connectFlags & MqttConnectMessage.FLAG_CONTAINS_USERNAME) ==
                MqttConnectMessage.FLAG_CONTAINS_USERNAME);
        underTest.toBytes();

    }
}