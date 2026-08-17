package org.example.event.restClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.exception.UserRestClientException;
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
public class ChatRestClient {

    private final IHttpCore httpCore;

    @Value("${app.integration.chat-service.base-url}")
    private String chatServiceBaseUrl;

    public void cancelChatRoom(Long eventId, Long currentUserId) {
        log.debug("[ [EVENT] - ChatRestClient ] Запрос на удаление комнаты чата для события ID: {}", eventId);
        try {
            String chatServiceUrl = chatServiceBaseUrl + "room/" + eventId + "/cancel";

            HttpHeaders headers = new HttpHeaders();
            headers.put("X-User-Id", List.of(String.valueOf(currentUserId)));
            HttpEntity<Void> entityRequest = new HttpEntity<>(headers);

            httpCore.post(chatServiceUrl, HttpMethod.DELETE, entityRequest, Void.class);

            log.info("[ChatRestClient - SUCCESS] Сигнал отмены встречи успешно доставлен в микросервис чатов");
        } catch (Exception e) {
            log.error("[ChatRestClient - ERROR] Не удалось связаться с сервисом чатов при отмене встречи {}. Причина: {}",
                    eventId, e.getMessage());
            throw new UserRestClientException("Не удалось отменить групповой чат встречи. Повторите попытку.");
        }
    }
}
