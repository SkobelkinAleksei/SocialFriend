package org.example.event.dto;

public record MyEventTabCountersDto(
        int upcoming,
        int past,
        int mineUpcoming,
        int minePast
) {}
