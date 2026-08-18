package org.example.notification.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.notification.entity.NotificationEntity;
import org.example.notification.exeption.NotificationAccessDeniedException;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.NotificationSettingsRepository;
import org.example.notification.util.NotificationAggregator;
import com.example.common.kafka.NotificationType;
import com.example.common.metrics.AppMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl — прочтение только своих уведомлений")
public class NotificationServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationSettingsRepository settingsRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationAggregator notificationAggregator;
    @Mock
    private AppMetrics appMetrics;
    @Mock
    private WebPushService webPushService;

    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    @DisplayName("Чужое уведомление нельзя пометить прочитанным")
    void markForeignForbidden() {
        NotificationEntity entity = NotificationEntity.builder()
                .id(1L)
                .receiverId(2L)
                .senderId(3L)
                .type(NotificationType.FRIEND_REQUEST_SENT)
                .read(false)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(NotificationAccessDeniedException.class, () -> service.markAsRead(1L, 9L));
        org.junit.jupiter.api.Assertions.assertFalse(entity.isRead());
    }

    @Test
    @DisplayName("Своё уведомление помечается read=true")
    void markOwn() {
        NotificationEntity entity = NotificationEntity.builder()
                .id(1L).receiverId(2L).senderId(3L)
                .type(NotificationType.POST_LIKE).read(false)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.markAsRead(1L, 2L);

        assertTrue(entity.isRead());
    }

    @Test
    @DisplayName("Нет уведомления — EntityNotFoundException")
    void missing() {
        when(repository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.markAsRead(8L, 1L));
    }

    @Test
    @DisplayName("Пустой список group-read — к БД не ходим")
    void emptyGroup() {
        service.markGroupAsRead(List.of(), 1L);
        verify(repository, never()).markIdsAsRead(any(), any());
    }

    @Test
    @DisplayName("Счётчик непрочитанных не включает сообщения чата")
    void unreadSkipsChat() {
        when(repository.countByReceiverIdAndReadFalseAndTypeNot(1L, NotificationType.NEW_CHAT_MESSAGE))
                .thenReturn(4L);
        org.junit.jupiter.api.Assertions.assertEquals(4L, service.countUnreadNotifications(1L));
    }
}
