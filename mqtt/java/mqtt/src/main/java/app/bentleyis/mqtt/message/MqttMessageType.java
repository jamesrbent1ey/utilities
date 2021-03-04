/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.message;

public enum MqttMessageType {
    CONNECT (1),
    CONNACK (2),
    PUBLISH (3),
    PUBACK (4),
    PUBREC (5),
    PUBREL (6),
    PUBCOMP (7),
    SUBSCRIBE (8),
    SUBACK (9),
    UNSUBSCRIBE (10),
    UNSUBACK (11),
    PINGREQ (12),
    PINGRESP (13),
    DISCONNECT (14);

    private final byte value;
    MqttMessageType(int value) {
        this.value = (byte)value;
    }
    
    public byte toByte() {
        return this.value;
    }

    public static MqttMessageType fromByte(byte value) {
        switch (value) {
            case 1:
                return CONNECT;
            case 2:
                return CONNACK;
            case 3:
                return PUBLISH;
            case 4:
                return PUBACK;
            case 5:
                return PUBREC;
            case 6:
                return PUBREL;
            case 7:
                return PUBCOMP;
            case 8:
                return SUBSCRIBE;
            case 9:
                return SUBACK;
            case 10:
                return UNSUBSCRIBE;
            case 11:
                return UNSUBACK;
            case 12:
                return PINGREQ;
            case 13:
                return PINGRESP;
            case 14:
                return DISCONNECT;
        }
        throw new IllegalArgumentException("valid values are 1-14");
    }
}
