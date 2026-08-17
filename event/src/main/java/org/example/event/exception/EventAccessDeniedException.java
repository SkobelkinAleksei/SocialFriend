package org.example.event.exception;

public class EventAccessDeniedException extends RuntimeException {
    public EventAccessDeniedException(String message) {
        super(message);
    }
}