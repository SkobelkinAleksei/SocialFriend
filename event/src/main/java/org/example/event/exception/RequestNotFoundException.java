package org.example.event.exception;

public class RequestNotFoundException extends ResourceNotFoundException {
    public RequestNotFoundException(Long participantId) {
        super(String.format("Заявка или участник с ID %d не найдены", participantId));
    }
}