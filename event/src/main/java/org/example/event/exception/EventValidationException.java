package org.example.event.exception;

public class EventValidationException extends RuntimeException {
    public EventValidationException(String message) {
        super(message);
    }
}