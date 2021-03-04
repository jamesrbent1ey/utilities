/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class MqttMessage {
    /** Maximum length for parameter strings */
    public static final int MAX_PARAMETER_LENGTH = 65535;

    private byte controlPacketType;
    private byte flags;
    protected int remainingLength;

    protected MqttMessage() {}

    protected void setControlPacketType(byte type) {
        controlPacketType = type;
    }

    protected void setFlags(byte flags) {
        this.flags = flags;
    }

    protected byte getControlPacketType() {
        return controlPacketType;
    }

    public MqttMessageType getMessageType() {
        return MqttMessageType.fromByte(getControlPacketType());
    }

    protected byte getFlags() {
        return flags;
    }

    /**
     * Convert the message to a byte array
     * @return message as an array of bytes
     * @throws IOException
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // write out the fixed header portion of the message. starting with the type and flags
        bos.write( (byte) ((controlPacketType << 4) | (flags & 0x0f)) );
        // add the encoded remaining length
        bos.write( encodeLength(remainingLength) );
        byte[] res = bos.toByteArray();
        bos.close();
        return res;
    }

    /**
     * Encode the given length via the MBI algorithm
     * @param length
     * @return
     * @throws IOException
     */
    private byte[] encodeLength(int length) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int numBytes =0;
        do {
            byte digit = (byte) (length % 128);
            length = length >> 7;
            if (length > 0) {
                digit = (byte) (digit | 128);
            }
            bos.write(digit);
        } while ( (length > 0) && (numBytes<4) );

        byte[] res = bos.toByteArray();
        bos.close();
        return res;
    }

    /**
     * Encode the string into the appropriate format: 2 bytes length followed by
     * the string data. This allows for a maximum string length of 65535 characters UTF-8 encoded.
     * @param s
     * @return
     */
    protected byte[] encodeString(String s) {
        int max = Math.min(s.length(), MAX_PARAMETER_LENGTH);
        byte[] bytes = new byte[2+max];
        bytes[0] = (byte)((max >> 8) & 0x00ff);
        bytes[1] = (byte)(max & 0x00ff);
        System.arraycopy(s.getBytes(),0, bytes, 2, max);
        return bytes;
    }
}
