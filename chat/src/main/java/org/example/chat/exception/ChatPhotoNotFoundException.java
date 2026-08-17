package org.example.chat.exception;

public class ChatPhotoNotFoundException extends RuntimeException {
    public ChatPhotoNotFoundException() {
        super("Фотография не найдена.");
    }
}
