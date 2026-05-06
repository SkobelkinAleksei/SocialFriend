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
    private String message;
}