package org.example.notification.service;

import com.example.common.kafka.NotificationDto;
import com.example.common.kafka.NotificationEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.entity.NotificationEntity;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void processNotification(NotificationEvent event) {
        NotificationEntity entity = NotificationEntity.builder()
                .receiverId(event.getReceiverId())
                .senderId(event.getSenderId())
                .type(event.getType())
                .targetId(event.getTargetId())
                .message(event.getMessage())
                .isRead(false)
                .build();

        repository.save(entity);
        log.info("[NotificationServiceImpl - INFO] Уведомление сохранено для пользователя {}", event.getReceiverId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return repository.findAllByReceiverId(userId, pageable)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = repository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Уведомление не найдено"));

        if (!notification.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете изменять чужие уведомления");
        }

        notification.setRead(true);
        log.info("[NotificationServiceImpl - INFO] Уведомление {} помечено как прочитанное", notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<NotificationEntity> unread = repository.findAllByReceiverIdAndIsReadFalse(userId);

        unread.forEach(n -> n.setRead(true));
        log.info("[NotificationServiceImpl - INFO] Все уведомления пользователя {} помечены как прочитанные", userId);
    }
}
