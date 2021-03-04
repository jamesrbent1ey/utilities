/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import app.bentleyis.mqtt.message.MqttConnAckMessage;
import app.bentleyis.mqtt.message.MqttConnectMessage;
import app.bentleyis.mqtt.message.MqttDisconnectMessage;
import app.bentleyis.mqtt.message.MqttMessage;
import app.bentleyis.mqtt.message.MqttMessageFactory;
import app.bentleyis.mqtt.message.MqttPingReqMessage;
import app.bentleyis.mqtt.message.MqttPingRespMessage;
import app.bentleyis.mqtt.message.MqttPubAckMessage;
import app.bentleyis.mqtt.message.MqttPubCompMessage;
import app.bentleyis.mqtt.message.MqttPubRecMessage;
import app.bentleyis.mqtt.message.MqttPubRelMessage;
import app.bentleyis.mqtt.message.MqttPublishMessage;
import app.bentleyis.mqtt.message.MqttSubAckMessage;
import app.bentleyis.mqtt.message.MqttSubscribeMessage;
import app.bentleyis.mqtt.message.MqttUnsubAckMessage;
import app.bentleyis.mqtt.message.MqttUnsubscribeMessage;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MqttProtocolHandlerTest {
    public static final String TESTTOPIC = "testtopic";
    public static final int TEST_PACKET_ID = 1358;
    public static final String TESTMESSAGE = "testmessage";
    public static final String TEST_CLIENT_ID = "testclientid";
    MqttProtocolHandler underTest;
    MqttContext mockContext;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttProtocolHandler();
        mockContext = mock(MqttContext.class);
    }

    @Test
    public void handleConnection() throws IOException {
        MqttConnectMessage connect = new MqttConnectMessage((byte)0,30, "test");
        ByteArrayInputStream bis = new ByteArrayInputStream(connect.toBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // need a mock for MqttContext
        when(mockContext.getClientId()).thenReturn(null);

        underTest.handleConnection(mockContext, bis, bos);

        byte[] resp = bos.toByteArray();
        assertNotNull(resp);
        MqttConnAckMessage ack = (MqttConnAckMessage) MqttMessageFactory.parseMessage(resp);
        assertEquals(MqttConnAckMessage.RETURN_CODE_ACCEPTED, ack.getReturnCode());

        bis.close();
        bos.close();
    }

    @Test
    public void publish() throws IOException {
        MqttPublishMessage publishMessage = new MqttPublishMessage(TESTTOPIC, TEST_PACKET_ID);
        publishMessage.setPayload(TESTMESSAGE.getBytes());

        when(mockContext.getMessageId()).thenReturn(1);

        // the result is just a publish message so test the interaction with context.
        byte[] framed = underTest.publish(mockContext, publishMessage);
        assertNotNull(framed);
        verify(mockContext, atLeastOnce()).getMessageId();
        try {
            MqttPublishMessage pub = (MqttPublishMessage) MqttMessageFactory.parseMessage(framed);
        } catch (Throwable t) {
            fail();
        }
        verify(mockContext, atLeastOnce()).setDelivered(anyInt(), any());

        publishMessage.setQos((byte) MqttPublishMessage.QOS_AT_LEAST_ONCE);
        underTest.publish(mockContext, publishMessage);
        verify(mockContext, atLeastOnce()).holdForAck(anyInt(),any());

        publishMessage.setQos((byte) MqttPublishMessage.QOS_EXACTLY_ONCE);
        underTest.publish(mockContext, publishMessage);
        verify(mockContext, atLeastOnce()).holdForRec(anyInt(),any());
    }

    @Test
    public void receive() throws IOException {
        // it would be better to break these up to allow parts of the test to complete should there
        // be issues with specific messages
        try {
            underTest.receive(mockContext, null);
            fail();
        } catch (IOException e) {
            //success
        }
        try {
            underTest.receive(mockContext, "".getBytes());
            fail();
        } catch (IOException e) {
            //success
        }
        try {
            underTest.receive(mockContext, new byte[2]);
            fail();
        } catch (Exception e) {
            //success
        }

        // connect
        MqttConnectMessage connectMessage = new MqttConnectMessage(MqttConnectMessage.FLAG_CLEAN_SESSION,
                60, TEST_CLIENT_ID);
        // conack
        MqttMessage connAckMessage = underTest.receive(mockContext, connectMessage.toBytes());
        assertNotNull(connAckMessage);
        assertTrue(connAckMessage instanceof MqttConnAckMessage);

        //publish
        MqttPublishMessage publishMessage = new MqttPublishMessage(TESTTOPIC, TEST_PACKET_ID);
        publishMessage.setPayload(TESTMESSAGE.getBytes());
        MqttMessage puback = underTest.receive(mockContext, publishMessage.toBytes());
        assertNull(puback);
        publishMessage.setQos((byte) MqttPublishMessage.QOS_AT_LEAST_ONCE);
        puback = underTest.receive(mockContext, publishMessage.toBytes());
        assertTrue(puback instanceof MqttPubAckMessage);
        publishMessage.setQos((byte) MqttPublishMessage.QOS_EXACTLY_ONCE);
        puback = underTest.receive(mockContext, publishMessage.toBytes());
        assertTrue(puback instanceof MqttPubRecMessage);
        // PubAck
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(TEST_PACKET_ID);
        assertNull(underTest.receive(mockContext, pubAckMessage.toBytes()));
        verify(mockContext,atLeastOnce()).setDelivered(anyInt(),any());
        // PubRel
        MqttPubRelMessage pubRelMessage = new MqttPubRelMessage(TEST_PACKET_ID);
        puback = underTest.receive(mockContext, pubRelMessage.toBytes());
        assertTrue(puback instanceof MqttPubCompMessage);
        verify(mockContext,atLeastOnce()).setDelivered(anyInt(),any());
        // PubRec
        MqttPubRecMessage pubRecMessage = new MqttPubRecMessage(TEST_PACKET_ID);
        puback = underTest.receive(mockContext, pubRecMessage.toBytes());
        assertTrue(puback instanceof MqttPubRelMessage);
        verify(mockContext,atLeastOnce()).holdForComp(anyInt(),any());
        // PubComp
        MqttPubCompMessage pubCompMessage = new MqttPubCompMessage(TEST_PACKET_ID);
        assertNull(underTest.receive(mockContext, pubCompMessage.toBytes()));
        verify(mockContext,atLeastOnce()).setDelivered(anyInt(),any());

        // pingreq
        MqttPingReqMessage ping = new MqttPingReqMessage();
        puback = underTest.receive(mockContext,ping.toBytes());
        assertTrue(puback instanceof MqttPingRespMessage);

        // subscribe
        MqttSubscribeMessage sub = new MqttSubscribeMessage(TEST_PACKET_ID);
        sub.addTopicFilter(TESTTOPIC, 0);
        puback = underTest.receive(mockContext, sub.toBytes());
        assertTrue(puback instanceof MqttSubAckMessage);
        verify(mockContext, atLeastOnce()).addListener(any(), anyInt());

        // unsubscribe
        MqttUnsubscribeMessage unsub = new MqttUnsubscribeMessage(TEST_PACKET_ID);
        unsub.addTopicFilter(TESTTOPIC);
        puback = underTest.receive(mockContext, unsub.toBytes());
        assertTrue(puback instanceof MqttUnsubAckMessage);
        verify(mockContext, atLeastOnce()).removeListener(any());


        // disconnect
        MqttDisconnectMessage dis = new MqttDisconnectMessage();
        puback = underTest.receive(mockContext, dis.toBytes());
        assertNull(puback);
        verify(mockContext, atLeastOnce()).close();
    }
}