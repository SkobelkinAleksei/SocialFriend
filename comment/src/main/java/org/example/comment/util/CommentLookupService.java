package org.example.comment.util;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.restclient.config.IHttpCore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentLookupService {

    private final IHttpCore iHttpCore;

    public PostDto getPostDtoFromApi(Long postId) {
        // 1. Формируем URL
        String url = "http://localhost:8080/api/v1/social/posts/id/%s".formatted(postId);

        // 2. Создаем объект RequestData
        RequestData requestData = new RequestData(url, null);

        // 3. Вызываем через iHttpCore
        ResponseEntity<PostDto> response = iHttpCore.get(requestData, PostDto.class);

        if (response.getBody() == null) {
            throw new EntityNotFoundException("Данные поста не найдены по запросу.");
        }

        return response.getBody();
    }
}
