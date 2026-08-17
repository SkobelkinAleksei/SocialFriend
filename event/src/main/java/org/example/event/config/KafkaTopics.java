package org.example.event.config;

public final class KafkaTopics {
    public static final String EVENT_CREATED = "event-created-topic";
    public static final String USER_JOINED = "event-user-joined-topic";
    public static final String USER_LEFT = "event-user-left-topic";
    public static final String USER_REPUTATION = "user-reputation-topic";

    private KafkaTopics() {
    }
}