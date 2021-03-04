/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttUnsubscribeMessageTest {
    private static final int PACKET_ID = 1;
    private static final String TOPIC = "testtopic";

    MqttUnsubscribeMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttUnsubscribeMessage(PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttUnsubscribeMessage(65536);
            fail();
        } catch (Exception e) {
            //success
        }
        try {
            underTest.toBytes();
            fail();
        } catch (Exception e) {
            //success
        }
    }

    @Test
    public void getPacketId() {
        assertEquals(PACKET_ID, underTest.getPacketIdentifier());
    }

    @Test
    public void addTopicFilter() {
        removeTopicFilter();

        underTest.addTopicFilter(TOPIC);
        underTest.addTopicFilter(TOPIC);
        assertEquals(1, underTest.getTopicFilters().size());
    }

    @Test
    public void removeTopicFilter() {
        assertTrue(underTest.getTopicFilters().isEmpty());

        underTest.addTopicFilter(TOPIC);
        assertFalse(underTest.getTopicFilters().isEmpty());

        underTest.removeTopicFilter(TOPIC);
        assertTrue(underTest.getTopicFilters().isEmpty());
    }

    @Test
    public void getTopicFilters() {
        removeTopicFilter();
    }

    @Test
    public void toBytes() throws IOException {
        // exception case handled in construction method above
        underTest.addTopicFilter(TOPIC);
        byte[] bytes = underTest.toBytes();

        assertEquals(PACKET_ID, bytes[3]);
        assertTrue(TOPIC.equals(new String(bytes,6,TOPIC.length())));
    }
}