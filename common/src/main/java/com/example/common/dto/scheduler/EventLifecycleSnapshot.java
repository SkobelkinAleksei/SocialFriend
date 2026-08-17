package com.example.common.dto.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLifecycleSnapshot {
    private boolean exists;
    private Long eventId;
    private Long organizerId;
    private String title;
    private LocalDateTime eventDate;
    private boolean started;
    @Builder.Default
    private int peopleCount = 0;
    @Builder.Default
    private List<Long> eligibleVoterIds = new ArrayList<>();
}
