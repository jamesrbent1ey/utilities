/*
 * Copyright (c) 2021. James Bentley, all rights reserved.
 */

package app.bentleyis.mqtt.example;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This example is an in-memory queue system. It will not persist between connections. This example
 * is meant to be very simple and is not meant to illustrate the most performant or robust implementation.
 *
 * A queue is unique for every client-topic tuple.
 */
public class Queue {
    String clientId;
    String topic;
    int keepAlive = Integer.MAX_VALUE;
    int qos;
    long lastAccessTime;
    int hash;
    LinkedList<byte[]> queue = new LinkedList<>();

    private static final LinkedList<Queue> allQueues = new LinkedList<>();

    public static synchronized List<Queue> getQueues(String clientId) {
        cleanUpQueues();

        LinkedList<Queue> queues = new LinkedList<>();
        for(Queue queue: allQueues) {
            if(queue.getClientId().equals(clientId)) {
                queues.add(queue);
            }
        }
        return queues;
    }

    public static synchronized Queue getQueue(String clientId, String topic) {
        cleanUpQueues();

        for(Queue queue: allQueues) {
            if(queue.getClientId().equals(clientId) && queue.getTopic().equals(topic)) {
                return queue;
            }
        }
        return null;
    }

    public static synchronized boolean dispatch(byte[] message, String topic) {
        cleanUpQueues();

        for(Queue queue: allQueues) {
            if(queue.getTopic().equals(topic)) {
                queue.queue.add(message);
                return true;
            }
        }

        // no queue - throw the packet away.
        return false;
    }

    public static synchronized void initQueue(String clientId, String topic, int keepAlive, int qos) {
        for(Queue queue: allQueues) {
            if(queue.getClientId().equals(clientId) && queue.getTopic().equals(topic)) {
                return;
            }
        }
        Queue queue = new Queue(clientId, topic, keepAlive, qos);
        allQueues.add(queue);
    }

    public static synchronized void releaseQueue(String clientId, String topic) {
        LinkedList<Queue> clone = new LinkedList<>(allQueues);
        for(Queue queue: clone) {
            if(queue.getTopic().equals(topic) && queue.getClientId().equals(clientId)) {
                allQueues.remove(queue);
            }
        }
    }

    private static synchronized void cleanUpQueues() {
        long time = System.currentTimeMillis();
        LinkedList<Queue> clone = new LinkedList<>(allQueues);
        for(Queue queue: clone) {
            if(queue.getKeepAlive() < (time - queue.getLastAccessTime())) {
                allQueues.remove(queue);
            }
        }
    }

    private Queue(String clientId, String topic, int keepAlive, int qos) {
        this.clientId = clientId;
        this.topic = topic;
        setKeepAlive(keepAlive);
        setQos(qos);
        this.hash = (clientId+topic).hashCode();
        lastAccessTime = System.currentTimeMillis();
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopic() {
        return topic;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long time) {
        lastAccessTime = time;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getQos() {
        return qos;
    }

    public byte[] peek() {
        lastAccessTime = System.currentTimeMillis();
        return queue.peekFirst();
    }

    public byte[] dequeue() {
        lastAccessTime = System.currentTimeMillis();
        return queue.removeFirst();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null || !(obj instanceof Queue)) {
            return false;
        }
        Queue other = (Queue) obj;
        return (other.getClientId().equals(getClientId()) && other.getTopic().equals(getTopic()));
    }
}
