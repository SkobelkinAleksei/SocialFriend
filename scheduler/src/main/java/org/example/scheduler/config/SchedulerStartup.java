package org.example.scheduler.config;

import lombok.RequiredArgsConstructor;
import org.example.scheduler.service.JobScheduleService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerStartup {

    private final JobScheduleService jobScheduleService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        jobScheduleService.ensureRefreshTokenCleanupJob();
    }
}
