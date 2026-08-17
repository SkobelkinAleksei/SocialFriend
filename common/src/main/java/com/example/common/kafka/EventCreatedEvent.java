package com.example.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreatedEvent {
    private Long eventId;
    private String title;
    private Long ownerId;
}