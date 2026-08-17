package com.example.common.kafka;

import com.example.common.kafka.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationGroupDto {
    private Long id;                     // ID самого последнего уведомления в группе
    private Long receiverId;             // Кто получает
    private Long senderId;               // От кого (последний автор)
    private NotificationType type;       // Тип уведомления
    private Long targetId;               // ID сущности (пост, чат и т.д.)
    private Long commentId;
    private String message;              // Текст, который увидит пользователь
    private boolean read;                // Статус прочтения
    private LocalDateTime createdAt;     // Время создания последнего уведомления
    private String senderFirstName;      // Имя последнего отправителя
    private String senderLastName;       // Фамилия последнего отправителя
    private String contextLabel;

    private int count;                   // Количество схлопнутых уведомлений
    private List<Long> mergedIds;        // Список всех ID из базы, которые вошли в эту группу

    private List<NotificationItemDetails> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationItemDetails {
        private Long id;
        private Long senderId;
        private Long commentId;
        private String senderFirstName;
        private String senderLastName;
        private String message;
        private LocalDateTime createdAt;
    }
}
