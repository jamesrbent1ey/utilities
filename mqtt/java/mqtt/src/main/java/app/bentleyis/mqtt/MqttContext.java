/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt;

import app.bentleyis.mqtt.message.MqttMessage;
import app.bentleyis.mqtt.message.MqttPublishMessage;

/**
 * The context for a call - this acts as a session
 * TODO - should separate this into different interfaces
 */
public interface MqttContext {
    /**
     * Set the client id associated with this context <Session></Session>
     * @param id
     */
    void setClientId(String id);

    /**
     * Get the client id associated with this context/session
     * @return stored client id, may be null
     */
    String getClientId();

    /**
     * distribute a message to a topic, copying/queuing the message
     * to all topic subscribers. Called by server.
     * @param topic
     * @param message
     */
    void distributeTo(String topic, MqttMessage message);

    /**
     * For qos 1, an Ack must be received. Hold the given message until acknowleged.
     * The Context should handle retry if ack not received or connection errors - i.e.
     * put the message back on the queue so that receiveFrom can be called, execute
     * MqttServer.getMessageFromTopic to handle the message. MqttServer will have already
     * updated the packet accordingly.  Qos 1
     * @param id
     * @param message
     */
    void holdForAck(int id, MqttPublishMessage message);

    /**
     * Get the message, by id, held for acknowledgement
     * @param id
     * @return null if not present
     */
    MqttPublishMessage getForAck(int id);

    /**
     * For Qos 2, a publish results in a received ack. Hold the message for the Rec packet
     * @see #holdForAck(int, MqttPublishMessage) for additional processing requirements
     * @param id
     * @param message
     */
    void holdForRec(int id, MqttPublishMessage message);

    /**
     * Get a message, by id, being held for reception acknowledgement
     * @param id
     * @return null if not present
     */
    MqttPublishMessage getForRec(int id);

    /**
     * For Qos 2, a PubRel package is acknowledged by a complete Message. Hold a message
     * for the complete acknowledgement.
     * @see #holdForAck(int, MqttPublishMessage) for additional processing requirments
     * @param id
     * @param message
     */
    void holdForComp(int id, MqttPublishMessage message);

    /**
     * Get the message being held for completion
     * @param id
     * @return
     */
    MqttPublishMessage getForComp(int id);

    /**
     * Called by Protocol handler to indicate successful delivery of a publish packet. The context
     * may dequeue the packet
     * @param id packet identifier
     * @param message this may be null if it was not properly managed or the acknowledging packet was erroneously received
     */
    void setDelivered(int id, MqttPublishMessage message);

    /**
     * Generate a unique message id for this context/session
     * @return unique message/packet identifier
     */
    int getMessageId();

    /**
     * Authorization
     */
    static final int AUTH_OK = 0;
    static final int AUTH_INVALID_CREDENTIALS = 1;
    static final int AUTH_NOT_AUTHORIZED = 2;

    /**
     * Handle authorization
     * @param uname may be null
     * @param password must not be null
     * @return one of AUTH_OK, AUTH_INVALID_CREDENTIALS or AUTH_NOT_AUTHORIZED
     */
    int auth(String uname, byte[] password);

    /**
     * Generate a client id if client did not specify
     * @return generated client id
     */
    String generateClientId();

    /**
     * Hold will message for error handling
     * @param message
     * @param qos
     * @param retain
     * @param willTopic
     */
    void setWill(byte[] message, int qos, boolean retain, String willTopic);
    int getWillQos();
    byte[] getWillMessage();
    boolean retainWill();
    String getWillTopic();

    /**
     * Listen for messages delivered to a topic
     * @param topic
     * @param qos
     */
    void addListener(String topic, int qos);

    /**
     * Stop listening for messages delivered to a topic
     * @param topic
     */
    void removeListener(String topic);

    /**
     * Indicate a ping processed
     */
    void ping();

    /**
     * End the session - can release context
     */
    void close();
}
