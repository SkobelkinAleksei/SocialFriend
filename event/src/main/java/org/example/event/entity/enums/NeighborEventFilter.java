package org.example.event.entity.enums;

public enum NeighborEventFilter {
    USER_UPCOMING,
    USER_PAST,
    /** Все встречи, которые сосед создал сам (и будущие, и прошедшие) */
    USER_CREATED
}