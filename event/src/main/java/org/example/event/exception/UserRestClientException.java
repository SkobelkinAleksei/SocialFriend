package org.example.event.exception;

public class UserRestClientException extends RuntimeException {
    public UserRestClientException(String message) {
        super(message);
    }
}