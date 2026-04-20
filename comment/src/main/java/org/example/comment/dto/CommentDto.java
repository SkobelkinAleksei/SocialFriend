package org.example.comment.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto implements Serializable {
    Long authorId;
    Long postId;
    String content;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}