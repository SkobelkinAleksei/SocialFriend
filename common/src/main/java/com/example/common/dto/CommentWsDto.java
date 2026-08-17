package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentWsDto {
    private Long id;
    private Long authorId;
    private Long postId;
    private String content;

    private String authorFirstName;
    private String authorLastName;
    private String authorAvatarUrl;
    /** COMMENT (по умолчанию) или VOTE */
    private String eventType;
    private Long likesCount;
    private Long dislikesCount;
    private Long replyToUserId;
    private String replyToAuthorName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
