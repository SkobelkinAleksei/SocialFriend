package com.example.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUserJoinedEvent {
    private Long eventId;
    private Long userId;
    private String firstName;
    private String lastName;
}
