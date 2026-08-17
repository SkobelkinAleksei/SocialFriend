package org.example.event.service;

import com.example.common.lifecycle.EventLifecycleSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.entity.EventEntity;
import org.example.event.repository.EventRepository;
import org.example.event.restClient.SchedulerRestClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventLifecycleReconcileJob {

    private final EventRepository eventRepository;
    private final SchedulerRestClient schedulerRestClient;
    private final EventLifecycleSettings lifecycleSettings;

    @Scheduled(fixedDelay = 900_000, initialDelay = 20_000)
    public void reconcile() {
        LocalDateTime from = LocalDateTime.now().minusMinutes(lifecycleSettings.getReputationCloseMinutes() + 60);
        List<EventEntity> events = eventRepository.findByEventDateGreaterThanEqual(from);
        for (EventEntity event : events) {
            try {
                schedulerRestClient.scheduleLifecycle(EventLifecycleService.toRequest(event));
            } catch (Exception e) {
                log.warn("[EventLifecycle] Не удалось сверить таймеры встречи {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
