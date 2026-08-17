package org.example.comment.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminCommentDto {
    Long id;
    Long authorId;
    Long postId;
    String targetType;
    Long targetId;
    String content;
    String status;
}
