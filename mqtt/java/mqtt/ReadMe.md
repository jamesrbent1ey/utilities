# MQTT for Android
MqttProtocolHandler is designed for both client and server use. MqttProtocolHandler takes packet based
messages and input/output streams. MqttContext must be implemented and provided to the MqttProtocolHandler.

MqttProtocolHandler's stream interface is expected primarily message reception and response. MqttProtocolHandler
provides a publish method that can be used to publish messages to remote entities (client or server).

The MqttContext implementation is responsible for distributing received publishes, managing subscriptions
and providing persistence (message state) for the MqttProtocolHandler. The MqttContext may also manage
connections or delegate such operations to other components.

# Instructions on Use
Provide connection (input and output streams) or packets directly to the MqttProtocolHandler, along with
an MqttContext.  The MqttProtocolHandler call's the MqttContext to distribute publishes and handle
subscriptions. The MqttContext is also responsible for handling connections.