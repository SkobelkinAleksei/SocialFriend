package org.example.event.exception;

public class EventNotFoundException extends ResourceNotFoundException {
    public EventNotFoundException(Long eventId) {
        super(String.format("Событие с ID %d не найдено", eventId));
    }
}