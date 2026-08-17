package org.example.scheduler.controller;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import lombok.RequiredArgsConstructor;
import org.example.scheduler.service.JobScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/scheduler")
@RequiredArgsConstructor
public class SchedulerInternalController {

    private final JobScheduleService jobScheduleService;

    @PostMapping("/events/lifecycle")
    public ResponseEntity<Void> upsertLifecycle(@RequestBody EventLifecycleRequest request) {
        jobScheduleService.upsertLifecycle(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/lifecycle/reset")
    public ResponseEntity<Void> resetLifecycle(@RequestBody EventLifecycleRequest request) {
        jobScheduleService.resetLifecycle(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/{eventId}/cancel")
    public ResponseEntity<Void> cancelLifecycle(@PathVariable Long eventId) {
        jobScheduleService.scheduleCancel(eventId);
        return ResponseEntity.ok().build();
    }
}
