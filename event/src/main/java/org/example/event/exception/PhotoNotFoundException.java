package org.example.event.exception;

public class PhotoNotFoundException extends ResourceNotFoundException {
    public PhotoNotFoundException() {
        super("Фотография не найдена.");
    }
}
