package com.example.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEvent {
    private Long receiverId;
    private Long senderId;
    private NotificationType type;
    private Long targetId;
    private Long commentId;
    private String message;
    private String senderFirstName;
    private String senderLastName;
    /** Для чатов: "PERSONAL" или "GROUP:Название встречи" */
    private String contextLabel;
    private Long replyToUserId;
    private String replyToAuthorName;
    private String authorAvatarUrl;
}