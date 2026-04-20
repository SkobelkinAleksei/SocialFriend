package org.example.notification.service;

import com.example.common.kafka.NotificationDto;
import com.example.common.kafka.NotificationEvent;

import java.util.List;

public interface NotificationService {
    void processNotification(NotificationEvent event);
    List<NotificationDto> getUserNotifications(Long userId, int page, int size);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
}