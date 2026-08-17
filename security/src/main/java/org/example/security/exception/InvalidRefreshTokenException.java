package org.example.security.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Недействительный refresh token");
    }
}
