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
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}