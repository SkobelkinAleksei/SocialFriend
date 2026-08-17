package org.example.notification.service;

import com.example.common.kafka.NotificationDto;
import com.example.common.kafka.NotificationEvent;
import com.example.common.kafka.NotificationGroupDto;
import com.example.common.kafka.UserSettingsUpdatedEvent;

import java.util.List;

public interface NotificationService {
    void processNotification(NotificationEvent event);
    List<NotificationGroupDto> getUserNotifications(Long userId, int page, int size);
    void markAsRead(Long notificationId, Long userId);
    void markGroupAsRead(List<Long> ids, Long userId);
    void markAllAsRead(Long userId);
    long countUnreadNotifications(Long userId);
    void updateLocalUserSettings(UserSettingsUpdatedEvent event);
    void initDefaultSettingsForNewUser(Long userId);
}