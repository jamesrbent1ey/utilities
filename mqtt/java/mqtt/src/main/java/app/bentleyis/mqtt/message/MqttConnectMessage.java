/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static app.bentleyis.mqtt.message.MqttMessageType.CONNECT;

/**
 * Generate a Connect Message
 */
public class MqttConnectMessage extends MqttMessage {
    public static final byte FLAG_CLEAN_SESSION = (byte)0x02;
    public static final byte FLAG_CONTAINS_WILL = (byte)0x04;
    public static final byte FLAG_WILL_QOS1 = (byte)0x08;
    public static final byte FLAG_WILL_QOS2 = (byte)0x10;
    public static final byte FLAG_RETAIN_WILL = (byte)0x20;
    public static final byte FLAG_CONTAINS_PASSWORD = (byte)0x40;
    public static final byte FLAG_CONTAINS_USERNAME = (byte)0x80;

    static byte[] protocolVersionAndName = {0x00, 0x04, 'M', 'Q', 'T', 'T'};
    static byte protocolLevel = (byte)0x04;
    byte connectFlags;
    int keepAlive;
    String userName;
    byte[] password;
    String willTopic;
    byte[] willMessage;
    String clientId;

    public MqttConnectMessage(byte connectFlags, int keepAlive, String clientId) {
        super();
        if(clientId == null || clientId.length() < 1) {
            throw new IllegalArgumentException("client Id must be greater than one");
        }
        setControlPacketType(CONNECT.toByte());
        this.connectFlags = connectFlags;
        this.keepAlive = keepAlive;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public byte getConnectFlags() {
        return connectFlags;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        if(userName == null) {
            connectFlags &= ~(FLAG_CONTAINS_USERNAME);
        } else  {
            connectFlags |= FLAG_CONTAINS_USERNAME;
        }
        this.userName = userName;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        if(password == null) {
            connectFlags &= ~(FLAG_CONTAINS_PASSWORD);
        } else  {
            connectFlags |= FLAG_CONTAINS_PASSWORD;
        }
        this.password = password;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWill(String willTopic, byte[] willMessage) {
        if(willTopic == null || willMessage == null) {
            connectFlags &= ~FLAG_CONTAINS_WILL;
        } else {
            connectFlags |= FLAG_CONTAINS_WILL;
        }
        this.willTopic = willTopic;
        // this leaves will message vulnerable to change.
        this.willMessage = willMessage;
    }

    public void setRetainWill(boolean retain) {
        if(retain) {
            connectFlags |= FLAG_RETAIN_WILL;
            return;
        }

        connectFlags &= ~FLAG_RETAIN_WILL;
    }

    public boolean retainWill() {
        return (connectFlags & FLAG_RETAIN_WILL) != 0;
    }

    public void setCleanSession(boolean clean) {
        if(clean) {
            connectFlags |= FLAG_CLEAN_SESSION;
        } else {
            connectFlags &= ~FLAG_CLEAN_SESSION;
        }
    }

    public boolean cleanSession() {
        return (connectFlags & FLAG_CLEAN_SESSION) != 0;
    }

    public void setWillQos(byte value) {
        value = (byte)(value & 0x03);
        connectFlags &= ~(FLAG_WILL_QOS1|FLAG_WILL_QOS2);
        connectFlags |= value << 3;
    }

    public byte getWillQos() {
        return (byte)((connectFlags >> 3) & 0x03);
    }

    private void addWill(ByteArrayOutputStream payloadBos) throws IOException {
        if((connectFlags & FLAG_CONTAINS_WILL) == 0 ||
           willMessage == null ||
           willTopic == null) {
            return;
        }

        // add the will topic
        byte[] topicBytes = encodeString(willTopic);
        payloadBos.write(topicBytes);

        // add will message
        int max = Math.min(willMessage.length, MAX_PARAMETER_LENGTH);
        // add the will message
        payloadBos.write( (byte)((max >> 8) & 0x00ff) );
        payloadBos.write( (byte)(max & 0x00ff) );
        payloadBos.write(willMessage,0,max);
    }

    private void addUserName(ByteArrayOutputStream payloadBos) throws IOException {
        if((connectFlags & FLAG_CONTAINS_USERNAME) == 0 || userName == null) {
            return;
        }
        byte[] unameBytes = encodeString(userName);
        payloadBos.write(unameBytes);
    }

    private void addPassword(ByteArrayOutputStream payloadBos) throws IOException {
        if((connectFlags & FLAG_CONTAINS_PASSWORD) == 0 || password == null) {
            return;
        }
        int max = Math.min(password.length,MAX_PARAMETER_LENGTH);
        // add the password
        payloadBos.write( (byte)((max >> 8) & 0x00ff) );
        payloadBos.write( (byte)(max & 0x00ff) );
        payloadBos.write(password,0,max);
    }

    @Override
    public byte[] toBytes() throws IOException {
        // this is the variable header and payload
        ByteArrayOutputStream payloadBos = new ByteArrayOutputStream();

        // this is the resulting final packet
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // add the version, name and level
        payloadBos.write(protocolVersionAndName);
        payloadBos.write(protocolLevel);

        // add the connect flags
        payloadBos.write(connectFlags);

        // add the 2 bytes of keepalive
        payloadBos.write((byte)((keepAlive>>8)&0x00ff));
        payloadBos.write((byte)(keepAlive&0x00ff));

        // add the client identifier
        byte[] clid = encodeString(clientId);
        payloadBos.write(clid);

        // The will (topic and message) precedes the user name. Add the will
        addWill(payloadBos);

        // User name precedes password. Add the user name
        addUserName(payloadBos);

        // Add the password
        addPassword(payloadBos);

        // now that we have the payload, we can get the remaining length
        byte[] payload = payloadBos.toByteArray();
        payloadBos.close();
        remainingLength = payload.length;

        //store the fixed header
        bos.write(super.toBytes());

        //add the variable header + payload
        bos.write(payload);

        byte[] result = bos.toByteArray();
        bos.close();

        return result;
    }
}
