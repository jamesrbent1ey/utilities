/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.example;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import app.bentleyis.mqtt.MqttContext;
import app.bentleyis.mqtt.MqttProtocolHandler;
import app.bentleyis.mqtt.message.MqttConnAckMessage;
import app.bentleyis.mqtt.message.MqttConnectMessage;
import app.bentleyis.mqtt.message.MqttDisconnectMessage;
import app.bentleyis.mqtt.message.MqttMessage;
import app.bentleyis.mqtt.message.MqttPublishMessage;
import app.bentleyis.mqtt.message.MqttSubscribeMessage;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

public class InMemServerTest {

    public static final String TESTSESSIONID = "testsessionid";
    public static final String TESTTOPIC = "testtopic";

    class Connection extends Thread {
        InputStream in;
        OutputStream out;
        InMemServer server = new InMemServer();
        public Connection(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public boolean isConnected() {
            return server.isConnected();
        }

        @Override
        public void run() {
            try {
                server.handleConnection(in,out,3600, TESTSESSIONID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Done!");
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void handleConnection() throws IOException {
        MqttProtocolHandler handler = new MqttProtocolHandler();
        PipedInputStream input = new PipedInputStream();
        PipedOutputStream toServer = new PipedOutputStream();

        PipedOutputStream output = new PipedOutputStream();
        PipedInputStream fromServer = new PipedInputStream();

        input.connect(toServer);
        fromServer.connect(output);

        Connection connection = new Connection(input, output);

        MqttConnectMessage connect = new MqttConnectMessage((byte) 0,
                3600, TESTSESSIONID);
        toServer.write(connect.toBytes());
        toServer.flush();
        connection.start();

        sleep(100);

        MqttContext mockContext = Mockito.mock(MqttContext.class);
        MqttMessage response = handler.handleConnection(mockContext,fromServer, toServer);
        // if a connack is processed, there is no response
        assertNull(response);

        sleep(100);

        // response should be null as ack does not need to be handled further.
        assertTrue(connection.isConnected());

        MqttSubscribeMessage subscribe = new MqttSubscribeMessage(1);
        subscribe.addTopicFilter(TESTTOPIC, 0);
        toServer.write(subscribe.toBytes());
        sleep(100);

        // If a suback is received then there should be no response necessary
        response = handler.handleConnection(mockContext,fromServer, toServer);
        assertNull(response);

        // now publish to the subscription
        MqttPublishMessage publish = new MqttPublishMessage(TESTTOPIC, 2);
        toServer.write(publish.toBytes());
        sleep(100);

        // now, we should get back our data
        response = handler.handleConnection(mockContext,fromServer, toServer);
        // TODO notice how the client can use Context.distributeTo to capture packets sent from the server
        Mockito.verify(mockContext, Mockito.atLeastOnce()).distributeTo(any(), any());
        // and again the response is nothing as there is no ack required due to qos setting

        MqttDisconnectMessage disconnect = new MqttDisconnectMessage();
        toServer.write(disconnect.toBytes());
        sleep(100);
        // server will close the connection so any attempt to handle from this point would throw
        try {
            handler.handleConnection(mockContext,fromServer,toServer);
            fail();
        } catch (Exception e) {
            // success
        }

        connection.interrupt();
    }
}