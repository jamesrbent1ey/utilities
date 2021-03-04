/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static app.bentleyis.mqtt.message.MqttMessageType.CONNACK;

public class MqttConnAckMessage extends MqttMessage {
    public static final int RETURN_CODE_ACCEPTED = 0;
    public static final int RETURN_CODE_INVALID_PROTO_VERSION = 1;
    public static final int RETURN_CODE_BAD_ID = 2; // client id rejected
    public static final int RETURN_CODE_SERVER_UNAVAIL = 3;
    public static final int RETURN_CODE_BAD_CREDENTIAL = 4; //Invalid user name or password
    public static final int RETURN_CODE_UNAUTHORIZED = 5;

    boolean sessionPresent;
    byte returnCode;

    public MqttConnAckMessage() {
        super();
        setControlPacketType(CONNACK.toByte());
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    public void setSessionPresent(boolean sessionPresent) {
        this.sessionPresent = sessionPresent;
    }

    public byte getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(byte returnCode) {
        this.returnCode = returnCode;
    }

    @Override
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        byte connectFlags = (byte)(sessionPresent?1:0);
        bodyStream.write(connectFlags);
        bodyStream.write(returnCode);
        byte[] result = bodyStream.toByteArray();
        bodyStream.close();

        remainingLength = result.length;
        bos.write(super.toBytes());
        bos.write(result);

        result = bos.toByteArray();
        bos.close();
        return result;
    }
}
