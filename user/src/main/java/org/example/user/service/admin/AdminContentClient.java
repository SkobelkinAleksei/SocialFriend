package org.example.user.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminContentClient {

    private final IHttpCore httpCore;

    @Value("${app.services.post-base-url:http://localhost:8083}")
    private String postBaseUrl;

    @Value("${app.services.event-base-url:http://localhost:8088}")
    private String eventBaseUrl;

    @Value("${app.services.chat-base-url:http://localhost:8087}")
    private String chatBaseUrl;

    @Value("${app.services.comment-base-url:http://localhost:8084}")
    private String commentBaseUrl;

    public void hidePost(Long adminId, Long postId) {
        delete(postBaseUrl + "/api/v1/social/admin/posts/" + postId, adminId);
    }

    public void hideComment(Long adminId, Long commentId) {
        delete(commentBaseUrl + "/api/v1/social/admin/comments/" + commentId, adminId);
    }

    public void cancelEvent(Long adminId, Long eventId) {
        delete(eventBaseUrl + "/api/v1/social/admin/events/" + eventId, adminId);
    }

    public void deleteMessage(Long adminId, Long messageId) {
        delete(chatBaseUrl + "/api/v1/social/admin/chats/messages/" + messageId, adminId);
    }

    private void delete(String url, Long adminId) {
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-User-Id", List.of(String.valueOf(adminId)));
        headers.put("X-Platform-Role", List.of("ADMIN"));
        try {
            httpCore.post(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.warn("[Admin] Не удалось удалить цель {}: {}", url, e.getMessage());
            throw new IllegalStateException("Не удалось удалить контент. Повторите попытку.");
        }
    }
}
