package org.example.event.outbox;

public enum OutboxStatus {
    PENDING,
    SENDING,
    SENT,
    DEAD
}
