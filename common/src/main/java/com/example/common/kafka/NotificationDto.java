package com.example.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private Long senderId;
    private NotificationType type;
    private Long targetId;
    private Long commentId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String senderFirstName;
    private String senderLastName;
    private String contextLabel;
}