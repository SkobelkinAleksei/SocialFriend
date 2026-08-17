package com.example.common.dto.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLifecycleRequest {
    private Long eventId;
    private Long organizerId;
    private String title;
    private LocalDateTime eventDate;
}
