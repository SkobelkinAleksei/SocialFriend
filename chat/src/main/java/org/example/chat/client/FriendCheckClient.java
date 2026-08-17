package org.example.chat.client;

import com.example.common.RequestData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendCheckClient {

    private final IHttpCore httpCore;

    @Value("${app.services.friend-base-url:http://localhost:8082}")
    private String friendBaseUrl;

    public boolean areFriends(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            return false;
        }
        try {
            String url = friendBaseUrl + "/api/v1/social/friends/public/check?userId1="
                    + userId1 + "&userId2=" + userId2;
            ResponseEntity<Boolean> response = httpCore.get(new RequestData(url), Boolean.class);
            return response != null && Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("[Chat] Не удалось проверить дружбу {} и {}", userId1, userId2, e);
            return false;
        }
    }

    public boolean isBlockedEitherWay(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            return false;
        }
        try {
            String url = friendBaseUrl + "/api/v1/social/friends/blocks/check?userId1="
                    + userId1 + "&userId2=" + userId2;
            ResponseEntity<Boolean> response = httpCore.get(new RequestData(url), Boolean.class);
            return response != null && Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("[Chat] Не удалось проверить блок {} и {}", userId1, userId2, e);
            return false;
        }
    }
}
