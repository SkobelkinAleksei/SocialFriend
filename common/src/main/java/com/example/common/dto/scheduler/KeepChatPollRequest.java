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
public class KeepChatPollRequest {
    @Builder.Default
    private List<Long> eligibleVoterIds = new ArrayList<>();
    private LocalDateTime closesAt;
}
