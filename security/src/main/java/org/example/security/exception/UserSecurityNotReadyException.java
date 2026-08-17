package org.example.security.exception;

public class UserSecurityNotReadyException extends RuntimeException {

    public UserSecurityNotReadyException(String message) {
        super(message);
    }
}
