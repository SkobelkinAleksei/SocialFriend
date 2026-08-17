package org.example.post.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class AdminPostDto {
    Long id;
    Long authorId;
    String content;
    List<String> photos;
    LocalDateTime createdAt;
    String statusPost;
    String hiddenReason;
    boolean commentsAllowed;
}
