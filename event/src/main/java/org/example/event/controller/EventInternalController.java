package org.example.event.controller;

import com.example.common.dto.scheduler.EventLifecycleSnapshot;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import org.example.event.service.EventLifecycleService;
import org.example.event.service.EventParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/events")
@RequiredArgsConstructor
public class EventInternalController {

    private final EventLifecycleService eventLifecycleService;
    private final EventParticipantService eventParticipantService;

    @GetMapping("/{eventId}/lifecycle")
    public EventLifecycleSnapshot snapshot(@PathVariable Long eventId) {
        return eventLifecycleService.snapshot(eventId);
    }

    @PostMapping("/{eventId}/started")
    public EventLifecycleSnapshot markStarted(@PathVariable Long eventId) {
        return eventLifecycleService.markStarted(eventId);
    }

    @PostMapping("/{eventId}/notify")
    public ResponseEntity<Void> notifyParticipants(
            @PathVariable Long eventId,
            @RequestParam NotificationType type
    ) {
        eventLifecycleService.notifyParticipants(eventId, type);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}/kick/{userId}")
    public ResponseEntity<Void> kickParticipant(@PathVariable Long eventId, @PathVariable Long userId) {
        eventParticipantService.kickParticipantInternal(eventId, userId);
        return ResponseEntity.ok().build();
    }
}
