/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MqttMessageTest {
    // MqttMessage is abstract, must have concrete implementation to test
    class MqttMessageImpl extends MqttMessage{
        public MqttMessageImpl(){
            super();
        }
    };

    MqttMessage underTest;

    @Before
    public void setup() {
        underTest = new MqttMessageImpl();
    }

    @Test
    public void setControlPacketType() {
        underTest.setControlPacketType((byte)1);
        assertEquals(1, underTest.getControlPacketType());
    }

    @Test
    public void setFlags() {
        underTest.setFlags((byte)1);
        assertEquals(1,underTest.getFlags());
    }

    @Test
    public void getControlPacketType() {
        assertEquals(0,underTest.getControlPacketType());
        setControlPacketType();
    }

    @Test
    public void getFlags() {
        assertEquals(0, underTest.getFlags());
        setFlags();
    }

    @Test
    public void toBytes() {
        // here we have multiple scenarios for how the remaining length is encoded
        // values less than 128 bytes, and values greater than 128 bytes
        try {
            underTest.remainingLength = 40;
            byte[] bytes = underTest.toBytes();
            assertTrue(bytes[1] == 40);

            // from spec, 321 should be encoded such that 321%128 is 65 so we set the
            // top bit of 65 which makes 193. We then take the value remaining (i.e. 321/128)
            // and set it in the byte following (as long as its smaller, which it is), so the
            // second byte is 2.  This then is the formula 2*128 + 65 = 321.  where 65 is recovered
            // by 193-128.
            underTest.remainingLength = 321;
            bytes = underTest.toBytes();
            assertTrue(bytes[1] == (byte)193);
            assertTrue(bytes[2] == 2);

            // no filling out to make the full 4 bytes
            underTest.remainingLength = 2097151;
            bytes = underTest.toBytes();
            assertTrue(bytes[1] == (byte)0xff);
            assertTrue(bytes[2] == (byte)0xff);
            assertTrue(bytes[3] == (byte)0x7f);

            // max length of payload (variable header + message)
            underTest.remainingLength = 268435455;
            bytes = underTest.toBytes();
            assertTrue(bytes[1] == (byte)0xff);
            assertTrue(bytes[2] == (byte)0xff);
            assertTrue(bytes[3] == (byte)0xff);
            assertTrue(bytes[4] == (byte)0x7f);
        } catch (IOException e) {
            fail("should not throw");
        }
    }

    @Test
    public void getMessageType() {
        try {
            underTest.getMessageType();
            fail();
        } catch (IllegalArgumentException e) {
            // success
        }
        underTest.setControlPacketType((byte)1);
        assertEquals(MqttMessageType.CONNECT,underTest.getMessageType());
    }
}