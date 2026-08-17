package org.example.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto implements Serializable {
    Long id;
    Long authorId;
    Long postId;
    String targetType;
    Long targetId;
    String content;
    String authorFirstName;
    String authorLastName;
    String authorAvatarUrl;
    Long replyToUserId;
    Long replyToCommentId;
    String replyToAuthorName;
    long likesCount;
    long dislikesCount;
    String myVote;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;
}
