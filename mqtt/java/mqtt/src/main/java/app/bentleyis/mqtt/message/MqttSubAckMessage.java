/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MqttSubAckMessage extends MqttMessage {
    public static final int RETURN_CODE_FAIL = 0x80;
    public static final int RETURN_CODE_SUCCESS_MAX_QOS0 = 0;
    public static final int RETURN_CODE_SUCCESS_MAX_QOS1 = 1;
    public static final int RETURN_CODE_SUCCESS_MAX_QOS2 = 2;

    int packetIdentifier; //2 bytes
    LinkedList<Integer> returnCodes = new LinkedList<>();

    public MqttSubAckMessage(int packetId) {
        super();
        setControlPacketType(MqttMessageType.SUBACK.toByte());
        if(packetId > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetId);
        }
        packetIdentifier = packetId;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public void addQosReturnCode(int pos, int code) {
        returnCodes.add(pos,code);
    }

    public void removeQosReturnCodeAt(int pos) {
        returnCodes.remove(pos);
    }

    public List<Integer> getQosReturnCodes() {
        return new LinkedList<>(returnCodes);
    }

    @Override
    public byte[] toBytes() throws IOException {
        if(returnCodes.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one return code");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // packet identifier
        payloadBos.write((byte)((packetIdentifier>>8)&0x00ff));
        payloadBos.write((byte)(packetIdentifier&0x00ff));

        for(int returnCode: returnCodes) {
            payloadBos.write((byte)(returnCode&0x00ff));
        }

        byte[] payload = payloadBos.toByteArray();
        payloadBos.close();

        remainingLength = payload.length;
        bos.write(super.toBytes());
        bos.write(payload);

        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
}
