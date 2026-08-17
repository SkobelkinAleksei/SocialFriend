package org.example.event.restClient;

import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRestClient {

    private final IHttpCore httpCore;

    @Value("${app.integration.user-service.base-url}")
    private String userServiceBaseUrl;

    public List<UserDto> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("[ [EVENT] - UserRestClient ] Пакетный запрос пользователей для ID: {}", userIds);
        try {
            String idsParam = userIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String userUrl = userServiceBaseUrl + "search/by-ids?ids=" + idsParam;

            RequestData requestData = new RequestData(userUrl);

            ResponseEntity<UserDto[]> response = httpCore.get(requestData, UserDto[].class);

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }

            log.warn("[ [EVENT] - UserRestClient] Микросервис user вернул ошибку при пакетном запросе. Статус: {}",
                    response != null ? response.getStatusCode() : "null");
        } catch (Exception e) {
            log.error("[ [EVENT] - UserRestClient - ERROR] Не удалось пакетно получить данные из user-service: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    public Optional<UserDto> getUserById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        List<UserDto> users = getUsersByIds(List.of(userId));
        return users.stream().findFirst();
    }
}