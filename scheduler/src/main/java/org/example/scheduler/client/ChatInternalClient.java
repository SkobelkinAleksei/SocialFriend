package org.example.scheduler.client;

import com.example.common.dto.scheduler.KeepChatPollRequest;
import com.example.common.dto.scheduler.SystemMessageRequest;
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
public class ChatInternalClient {

    private final IHttpCore httpCore;

    @Value("${app.services.chat-base-url}")
    private String chatBaseUrl;

    public void postSystemMessage(Long eventId, String content) {
        String url = chatBaseUrl + "/api/v1/internal/chats/room/" + eventId + "/system-message";
        ignoreMissingRoom(() -> httpCore.post(
                url, HttpMethod.POST, new HttpEntity<>(new SystemMessageRequest(content)), Void.class));
    }

    public void createKeepPoll(Long eventId, KeepChatPollRequest request) {
        String url = chatBaseUrl + "/api/v1/internal/chats/room/" + eventId + "/keep-poll";
        ignoreMissingRoom(() -> httpCore.post(url, HttpMethod.POST, new HttpEntity<>(request), Void.class));
    }

    public boolean resolveKeepPoll(Long eventId) {
        try {
            String url = chatBaseUrl + "/api/v1/internal/chats/room/" + eventId + "/resolve-keep-poll";
            ResponseEntity<Boolean> response = httpCore.post(url, HttpMethod.POST, HttpEntity.EMPTY, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return true;
            }
            throw ex;
        }
    }

    public void closeRoom(Long eventId) {
        String url = chatBaseUrl + "/api/v1/internal/chats/room/" + eventId + "/close";
        ignoreMissingRoom(() -> httpCore.post(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class));
    }

    public void deleteRoom(Long eventId) {
        String url = chatBaseUrl + "/api/v1/internal/chats/room/" + eventId;
        ignoreMissingRoom(() -> httpCore.post(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class));
    }

    private void ignoreMissingRoom(Runnable call) {
        try {
            call.run();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return;
            }
            throw ex;
        }
    }
}
