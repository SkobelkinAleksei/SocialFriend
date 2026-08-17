package org.example.scheduler.client;

import com.example.common.RequestData;
import com.example.common.dto.scheduler.EventLifecycleSnapshot;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class EventInternalClient {

    private final IHttpCore httpCore;

    @Value("${app.services.event-base-url}")
    private String eventBaseUrl;

    public EventLifecycleSnapshot snapshot(Long eventId) {
        try {
            String url = eventBaseUrl + "/api/v1/internal/events/" + eventId + "/lifecycle";
            ResponseEntity<EventLifecycleSnapshot> response = httpCore.get(new RequestData(url), EventLifecycleSnapshot.class);
            if (response.getBody() == null) {
                return EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build();
            }
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build();
            }
            throw ex;
        }
    }

    public EventLifecycleSnapshot markStarted(Long eventId) {
        try {
            String url = eventBaseUrl + "/api/v1/internal/events/" + eventId + "/started";
            ResponseEntity<EventLifecycleSnapshot> response =
                    httpCore.post(url, HttpMethod.POST, HttpEntity.EMPTY, EventLifecycleSnapshot.class);
            return response.getBody() != null
                    ? response.getBody()
                    : EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build();
            }
            throw ex;
        }
    }

    public void notifyParticipants(Long eventId, NotificationType type) {
        try {
            String url = eventBaseUrl + "/api/v1/internal/events/" + eventId + "/notify?type=" + type.name();
            httpCore.post(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw ex;
        }
    }
}
