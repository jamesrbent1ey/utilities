/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MqttMessageFactoryTest {
    private static final String CLIENT_ID = "testclientid";
    private static final String TEST_PASSWORD = "testpassword";
    private static final String TEST_USER_NAME = "testusername";
    private static final String TEST_WILL_TOPIC = "testwilltopic";
    private static final String TEST_WILL_MESSAGE = "testwillmessage";
    private static final String TEST_PAYLOAD1 = "TestPayload";
    private static final byte[] TEST_PAYLOAD256 = new byte[256];

    private static final int TEST_PACKET_ID = 1;

    @Test
    public void parseMessage() throws IOException {
        assertNull(MqttMessageFactory.parseMessage(null));
        assertNull(MqttMessageFactory.parseMessage(new byte[0]));
        byte[] garbage = new byte[12];
        try {
            MqttMessageFactory.parseMessage(garbage);
            fail();
        } catch (Exception e) {
            // success
        }
    }

    @Test
    public void parseConnectMessage() throws IOException {
        MqttConnectMessage subject = new MqttConnectMessage((byte) 0,30, CLIENT_ID);
        subject.setPassword(TEST_PASSWORD.getBytes());
        subject.setUserName(TEST_USER_NAME);
        subject.setWill(TEST_WILL_TOPIC,TEST_WILL_MESSAGE.getBytes());
        MqttConnectMessage res = (MqttConnectMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertTrue(TEST_PASSWORD.equals(new String(res.getPassword())));
        assertTrue(TEST_USER_NAME.equals(res.getUserName()));
        assertTrue(TEST_WILL_TOPIC.equals(res.getWillTopic()));
        assertTrue(TEST_WILL_MESSAGE.equals(new String(res.getWillMessage())));
    }
    @Test
    public void parseConnAck() throws IOException {
        MqttConnAckMessage subject = new MqttConnAckMessage();
        MqttConnAckMessage res = (MqttConnAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        subject.setReturnCode((byte)1);
        subject.setSessionPresent(true);
        res = (MqttConnAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertTrue(res.sessionPresent);
        assertEquals(1, res.getReturnCode());
    }
    @Test
    public void parsePublish() throws IOException {
        MqttPublishMessage subject = new MqttPublishMessage(TEST_WILL_TOPIC,TEST_PACKET_ID);
        MqttPublishMessage res = (MqttPublishMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);

        subject.setPayload(TEST_PAYLOAD1.getBytes());
        res = (MqttPublishMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertTrue(TEST_PAYLOAD1.equals(new String(res.getPayload())));

        subject.setPayload(TEST_PAYLOAD256);
        res = (MqttPublishMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertEquals(TEST_PAYLOAD256.length, res.getPayload().length);
    }
    @Test
    public void parsePubAck() throws IOException {
        MqttPubAckMessage subject = new MqttPubAckMessage(TEST_PACKET_ID);
        MqttPubAckMessage res = (MqttPubAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
    }
    @Test
    public void parsePubRec() throws IOException {
        MqttPubRecMessage subject = new MqttPubRecMessage(TEST_PACKET_ID);
        MqttPubRecMessage res = (MqttPubRecMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
    }
    @Test
    public void parsePubRel() throws IOException {
        MqttPubRelMessage subject = new MqttPubRelMessage(TEST_PACKET_ID);
        MqttPubRelMessage res = (MqttPubRelMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
    }
    @Test
    public void parsePubComp() throws IOException {
        MqttPubCompMessage subject = new MqttPubCompMessage(TEST_PACKET_ID);
        MqttPubCompMessage res = (MqttPubCompMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
    }
    @Test
    public void parseSubscribe() throws IOException {
        MqttSubscribeMessage subject = new MqttSubscribeMessage(TEST_PACKET_ID);
        MqttSubscribeMessage res = null;
        try {
            res = (MqttSubscribeMessage) MqttMessageFactory.parseMessage(subject.toBytes());
            fail();
        } catch (IllegalArgumentException e) {
            //success
        }
        subject.addTopicFilter(TEST_WILL_TOPIC,0);
        res = (MqttSubscribeMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);

        Map<String,Integer> filters = res.getTopicFilters();
        assertEquals(1,filters.size());
        assertEquals(0, (int)filters.get(TEST_WILL_TOPIC));
    }
    @Test
    public void parseSubAck() throws IOException {
        MqttSubAckMessage subject = new MqttSubAckMessage(TEST_PACKET_ID);
        MqttSubAckMessage res = null;
        try {
            res = (MqttSubAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
            fail();
        } catch (IllegalArgumentException e) {
            // success
        }
        subject.addQosReturnCode(0, 0x80);
        res = (MqttSubAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
        List<Integer> rcs = res.getQosReturnCodes();
        assertEquals(1, rcs.size());
        assertEquals(-128, (int)rcs.get(0));

        subject.addQosReturnCode(1, 0);
        res = (MqttSubAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        rcs = res.getQosReturnCodes();
        assertEquals(2, rcs.size());
        assertEquals(0, (int)rcs.get(1));
    }
    @Test
    public void parseUnsubscribe() throws IOException {
        MqttUnsubscribeMessage subject = new MqttUnsubscribeMessage(TEST_PACKET_ID);
        MqttUnsubscribeMessage res = null;
        try {
            res = (MqttUnsubscribeMessage) MqttMessageFactory.parseMessage(subject.toBytes());
            fail();
        } catch(IllegalArgumentException e) {
            // success
        }
        subject.addTopicFilter(TEST_WILL_TOPIC);
        res = (MqttUnsubscribeMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        List<String> filters = res.getTopicFilters();
        assertTrue(TEST_WILL_TOPIC.equals(filters.get(0)));

        subject.addTopicFilter(TEST_WILL_MESSAGE);
        res = (MqttUnsubscribeMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        filters = res.getTopicFilters();
        assertEquals(2,filters.size());
    }
    @Test
    public void parseUnsubAck() throws IOException {
        MqttUnsubAckMessage subject = new MqttUnsubAckMessage(TEST_PACKET_ID);
        MqttUnsubAckMessage res = (MqttUnsubAckMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
        assertEquals(TEST_PACKET_ID, res.getPacketIdentifier());
    }
    @Test
    public void parsePingReq() throws IOException {
        MqttPingReqMessage subject = new MqttPingReqMessage();
        MqttPingReqMessage res = (MqttPingReqMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
    }
    @Test
    public void parsePingResp() throws IOException {
        MqttPingRespMessage subject = new MqttPingRespMessage();
        MqttPingRespMessage res = (MqttPingRespMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
    }
    @Test
    public void parseDisconnect() throws IOException {
        MqttDisconnectMessage subject = new MqttDisconnectMessage();
        MqttDisconnectMessage res = (MqttDisconnectMessage) MqttMessageFactory.parseMessage(subject.toBytes());
        assertNotNull(res);
    }
}