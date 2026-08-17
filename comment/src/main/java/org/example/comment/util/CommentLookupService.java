package org.example.comment.util;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.comment.dto.PhotoCommentTargetDto;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentLookupService {

    private final IHttpCore iHttpCore;

    @Value("${app.services.post-base-url:http://localhost:8083}")
    private String postBaseUrl;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    public PostDto getPostDtoFromApi(Long postId, Long viewerId) {
        String url = postBaseUrl + "/api/v1/social/posts/id/" + postId;
        RequestData requestData = new RequestData(
                url,
                Map.of("X-User-Id", String.valueOf(viewerId))
        );

        ResponseEntity<PostDto> response = iHttpCore.get(requestData, PostDto.class);

        if (response.getBody() == null) {
            throw new EntityNotFoundException("Данные поста не найдены по запросу.");
        }

        return response.getBody();
    }

    public PhotoCommentTargetDto getPhotoCommentTarget(String kind, Long targetId, Long viewerId) {
        String url = userBaseUrl + "/api/v1/internal/users/comment-target?kind=" + kind + "&targetId=" + targetId;
        RequestData requestData = new RequestData(
                url,
                Map.of("X-User-Id", String.valueOf(viewerId))
        );
        ResponseEntity<PhotoCommentTargetDto> response = iHttpCore.get(requestData, PhotoCommentTargetDto.class);
        if (response == null || response.getBody() == null) {
            throw new EntityNotFoundException("Фото не найдено.");
        }
        return response.getBody();
    }
}
