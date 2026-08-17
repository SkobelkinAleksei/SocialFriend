package org.example.user.client;

import com.example.common.RequestData;
import com.example.common.dto.BlockStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendBlockClient {

    private final IHttpCore httpCore;

    @Value("${app.services.friend-base-url:http://localhost:8082}")
    private String friendBaseUrl;

    public BlockStatusDto status(Long viewerId, Long targetUserId) {
        if (viewerId == null || targetUserId == null || viewerId.equals(targetUserId)) {
            return new BlockStatusDto(false, false);
        }
        try {
            String url = friendBaseUrl + "/api/v1/social/friends/blocks/status/" + targetUserId;
            RequestData requestData = new RequestData(url, java.util.Map.of("X-User-Id", String.valueOf(viewerId)));
            ResponseEntity<BlockStatusDto> response = httpCore.get(requestData, BlockStatusDto.class);
            if (response != null && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("[User] Не удалось проверить блок {} ↔ {}: {}", viewerId, targetUserId, e.getMessage());
        }
        return new BlockStatusDto(false, false);
    }

    public Set<Long> hiddenUserIds(Long viewerId) {
        if (viewerId == null) {
            return Set.of();
        }
        try {
            String url = friendBaseUrl + "/api/v1/social/friends/blocks/hidden-ids";
            RequestData requestData = new RequestData(url, java.util.Map.of("X-User-Id", String.valueOf(viewerId)));
            ResponseEntity<Long[]> response = httpCore.get(requestData, Long[].class);
            if (response != null && response.getBody() != null) {
                return new HashSet<>(java.util.Arrays.asList(response.getBody()));
            }
        } catch (Exception e) {
            log.warn("[User] Не удалось получить скрытых соседей для {}: {}", viewerId, e.getMessage());
        }
        return Collections.emptySet();
    }
}
