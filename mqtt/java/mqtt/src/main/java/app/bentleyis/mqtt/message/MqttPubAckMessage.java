/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MqttPubAckMessage extends MqttMessage {

    int packetIdentifier; //2 bytes

    public MqttPubAckMessage(int packetId) {
        super();
        setControlPacketType(MqttMessageType.PUBACK.toByte());
        if(packetId > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetId);
        }
        packetIdentifier = packetId;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        remainingLength = 2;
        bos.write(super.toBytes());
        bos.write((byte)((packetIdentifier>>8)&0x00ff));
        bos.write((byte)(packetIdentifier&0x00ff));
        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
}
