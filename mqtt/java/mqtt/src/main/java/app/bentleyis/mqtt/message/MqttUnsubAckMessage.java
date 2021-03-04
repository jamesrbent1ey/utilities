/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MqttUnsubAckMessage extends MqttMessage {

    int packetIdentifier;

    public MqttUnsubAckMessage(int packetIdentifier) {
        super();
        if(packetIdentifier > 65535) {
            throw new IllegalArgumentException("Packet ID too large: "+packetIdentifier);
        }
        setControlPacketType(MqttMessageType.UNSUBACK.toByte());
        this.packetIdentifier = packetIdentifier;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // packet identifier
        payloadBos.write((byte)((packetIdentifier >> 8)&0x00ff));
        payloadBos.write((byte)(packetIdentifier&0x00ff));

        byte[] payload = payloadBos.toByteArray();
        payloadBos.close();

        // assemble packet
        remainingLength = payload.length;
        bos.write(super.toBytes());
        bos.write(payload);

        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }
}
