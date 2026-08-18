package org.example.security.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Подтвердите почту: код из письма на экране регистрации.");
    }
}
