package org.example.like.util;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikePostLookupService {

    private final IHttpCore iHttpCore;

    @Value("${app.services.post-base-url:http://localhost:8083}")
    private String postBaseUrl;

    public PostDto getPostDtoFromApi(Long postId, Long viewerId) {
        String url = postBaseUrl + "/api/v1/social/posts/id/" + postId;
        RequestData requestData = new RequestData(
                url,
                Map.of("X-User-Id", String.valueOf(viewerId))
        );

        ResponseEntity<PostDto> response = iHttpCore.get(requestData, PostDto.class);

        if (response == null || response.getBody() == null) {
            throw new EntityNotFoundException("Данные поста не найдены по запросу.");
        }

        return response.getBody();
    }
}
