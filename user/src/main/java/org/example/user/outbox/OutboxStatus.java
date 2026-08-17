package org.example.user.outbox;

public enum OutboxStatus {
    PENDING,
    SENDING,
    SENT,
    DEAD
}
