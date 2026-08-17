package org.example.event.exception;

public class ParticipantLimitExceededException extends RuntimeException {
    public ParticipantLimitExceededException() {
        super("Лимит участников на данное событие исчерпан");
    }
}