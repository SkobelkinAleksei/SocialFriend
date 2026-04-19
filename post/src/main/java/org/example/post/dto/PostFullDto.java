package org.example.post.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PostFullDto implements Serializable {
    Long authorId;
    String content;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime publishAt;
}