package org.example.notification.exeption;

public class NotificationAccessDeniedException extends RuntimeException {
    public NotificationAccessDeniedException(String message) {
        super(message);
    }
}