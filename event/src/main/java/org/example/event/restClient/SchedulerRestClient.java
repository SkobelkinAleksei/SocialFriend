package org.example.event.restClient;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.exception.UserRestClientException;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerRestClient {

    private final IHttpCore httpCore;

    @Value("${app.integration.scheduler-service.base-url}")
    private String schedulerBaseUrl;

    public void scheduleLifecycle(EventLifecycleRequest request) {
        try {
            httpCore.post(schedulerBaseUrl + "events/lifecycle", HttpMethod.POST, new HttpEntity<>(request), Void.class);
        } catch (Exception e) {
            log.error("[SchedulerRestClient] Не удалось поставить таймеры встречи {}: {}",
                    request != null ? request.getEventId() : null, e.getMessage());
        }
    }

    public void resetLifecycle(EventLifecycleRequest request) {
        try {
            httpCore.post(schedulerBaseUrl + "events/lifecycle/reset", HttpMethod.POST, new HttpEntity<>(request), Void.class);
        } catch (Exception e) {
            log.error("[SchedulerRestClient] Не удалось перенести таймеры встречи {}: {}",
                    request != null ? request.getEventId() : null, e.getMessage());
            throw new UserRestClientException("Не удалось перенести таймеры встречи. Повторите попытку.");
        }
    }

    public void cancelLifecycle(Long eventId) {
        try {
            httpCore.post(schedulerBaseUrl + "events/" + eventId + "/cancel", HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (Exception e) {
            log.error("[SchedulerRestClient] Не удалось запланировать удаление чата встречи {}: {}", eventId, e.getMessage());
            throw new UserRestClientException("Не удалось запланировать удаление чата. Повторите попытку.");
        }
    }
}
