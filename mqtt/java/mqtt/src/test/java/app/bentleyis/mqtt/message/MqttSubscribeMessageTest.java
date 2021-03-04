/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import android.app.MediaRouteActionProvider;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class MqttSubscribeMessageTest {
    private static final int PACKET_ID = 1;
    private static final String TOPIC_NAME = "testtopic";
    private static final int TOPIC_QOS = 1;

    MqttSubscribeMessage underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new MqttSubscribeMessage(PACKET_ID);
    }

    @Test
    public void construction() {
        try {
            underTest = new MqttSubscribeMessage(65536);
            fail();
        } catch (Exception e) {
            // success
        }
        try {
            underTest.toBytes();
            fail();
        } catch (Exception e) {
            // success, no topic filters.
        }
    }

    @Test
    public void getPacketId() {
        assertEquals(PACKET_ID, underTest.getPacketId());
    }

    @Test
    public void addTopicFilter() {
        removeTopicFilter();
    }

    @Test
    public void removeTopicFilter() {
        Map<String,Integer> filters = underTest.getTopicFilters();
        assertTrue(filters.isEmpty());

        underTest.addTopicFilter(TOPIC_NAME,TOPIC_QOS);
        filters = underTest.getTopicFilters();
        assertFalse(filters.isEmpty());

        underTest.removeTopicFilter(TOPIC_NAME);
        filters = underTest.getTopicFilters();
        assertTrue(filters.isEmpty());
    }

    @Test
    public void getTopicFilters() {
        removeTopicFilter();
    }

    @Test
    public void toBytes() throws IOException {
        //exception case handled in construction method
        underTest.addTopicFilter(TOPIC_NAME,TOPIC_QOS);
        byte[] bytes = underTest.toBytes();
        assertEquals(PACKET_ID,bytes[3]);
        assertTrue(TOPIC_NAME.equals(new String(bytes,6, TOPIC_NAME.length())));
    }
}