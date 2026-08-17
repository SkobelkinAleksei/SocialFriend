package org.example.chat.entity.admin;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AdminChatLineDto {
    Long id;
    Long authorId;
    String authorName;
    String text;
    LocalDateTime timestamp;
    boolean deleted;
    boolean highlighted;
}
