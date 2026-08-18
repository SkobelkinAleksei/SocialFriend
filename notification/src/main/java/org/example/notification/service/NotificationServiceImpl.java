package org.example.notification.service;

import com.example.common.dto.UserSettingsDto;
import com.example.common.kafka.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.entity.NotificationEntity;
import org.example.notification.entity.NotificationSettingsEntity;
import org.example.notification.exeption.NotificationAccessDeniedException;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.NotificationSettingsRepository;
import org.example.notification.util.NotificationAggregator;
import org.example.notification.util.NotificationConstants;
import com.example.common.metrics.AppMetrics;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository repository;
    private final NotificationSettingsRepository settingsRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationAggregator notificationAggregator;
    private final AppMetrics appMetrics;
    private final WebPushService webPushService;

    @Override
    @Transactional
    public void processNotification(NotificationEvent event) {
        log.info("[NotificationService] Получено событие {} для пользователя {}", event.getType(), event.getReceiverId());

        // 1. Дизлайк: чистим все like-нотификации (read/unread) и всегда шлём sync в сокет
        if (isLikeType(event.getType())
                && NotificationConstants.SYSTEM_LIKE_REMOVED.equals(event.getMessage())) {

            int deleted = repository.deleteByReceiverSenderTypeTarget(
                    event.getReceiverId(),
                    event.getSenderId(),
                    event.getType(),
                    event.getTargetId()
            );
            log.info("[Анти-спам] Удалено уведомлений о лайке: {} (receiver={}, sender={}, post={})",
                    deleted, event.getReceiverId(), event.getSenderId(), event.getTargetId());

            sendLikeSocketSync(event, NotificationConstants.SYSTEM_LIKE_REMOVED);
            return;
        }

        // 2. Настройки тостов: sync счётчика лайков всё равно уходит
        boolean toastEnabled = checkRemoteUserSettings(event.getReceiverId(), event.getType());
        if (!toastEnabled) {
            if (isLikeType(event.getType())) {
                sendLikeSocketSync(event, NotificationConstants.SYSTEM_LIKE_SYNC);
            }
            log.info("[NotificationService] Тост типа {} заблокирован настройками {}. Sync отправлен при необходимости.",
                    event.getType(), event.getReceiverId());
            return;
        }

        // 3. Идентичные непрочитанные (заявки на встречу и т.п.) — одна строка, обновляем текст
        if (isCollapsibleIdentityType(event.getType())) {
            Optional<NotificationEntity> existing = repository
                    .findByReceiverIdAndSenderIdAndTypeAndTargetIdAndReadFalse(
                            event.getReceiverId(), event.getSenderId(), event.getType(), event.getTargetId()
                    );
            if (existing.isPresent()) {
                NotificationEntity entityToUpdate = existing.get();
                entityToUpdate.setMessage(event.getMessage());
                entityToUpdate.setContextLabel(event.getContextLabel());
                entityToUpdate.setSenderFirstName(event.getSenderFirstName());
                entityToUpdate.setSenderLastName(event.getSenderLastName());
                if (event.getCommentId() != null) {
                    entityToUpdate.setCommentId(event.getCommentId());
                }
                NotificationEntity saved = repository.save(entityToUpdate);
                appMetrics.uvedomlenieSohraneno();
                pushToUser(event.getReceiverId(), notificationMapper.toDto(saved));
                log.info("[Анти-дубль] Обновлено существующее уведомление type={} target={}", event.getType(), event.getTargetId());
                return;
            }
        }

        if (isOncePerTargetType(event.getType()) && event.getTargetId() != null
                && repository.existsByReceiverIdAndTypeAndTargetId(
                event.getReceiverId(), event.getType(), event.getTargetId())) {
            log.info("[Анти-дубль] Пропуск повторного {} для user={} target={}",
                    event.getType(), event.getReceiverId(), event.getTargetId());
            return;
        }

        // 4. Повторный лайк при непрочитанном: схлопываем БД, но sync в сокет оставляем
        if (isLikeType(event.getType())) {
            Optional<NotificationEntity> existing = repository
                    .findByReceiverIdAndSenderIdAndTypeAndTargetIdAndReadFalse(
                            event.getReceiverId(), event.getSenderId(), event.getType(), event.getTargetId()
                    );

            if (existing.isPresent()) {
                NotificationEntity entityToUpdate = existing.get();
                entityToUpdate.setMessage(event.getMessage());
                repository.save(entityToUpdate);
                log.info("[Анти-спам] Повторный лайк от {} схлопнут в БД. Шлём только sync.", event.getSenderId());
                sendLikeSocketSync(event, NotificationConstants.SYSTEM_LIKE_SYNC);
                return;
            }
        }

        NotificationEntity entityToSave = NotificationEntity.builder()
                .receiverId(event.getReceiverId())
                .senderId(event.getSenderId())
                .type(event.getType())
                .targetId(event.getTargetId())
                .commentId(event.getCommentId())
                .message(event.getMessage())
                .senderFirstName(event.getSenderFirstName())
                .senderLastName(event.getSenderLastName())
                .contextLabel(event.getContextLabel())
                .read(false)
                .build();

        if (isLikeType(event.getType()) && event.getTargetId() == null) {
            log.error("[NotificationService] {} без targetId. Событие отброшено: {}", event.getType(), event);
            return;
        }

        NotificationEntity saved = repository.save(entityToSave);
        appMetrics.uvedomlenieSohraneno();
        log.info("[Уведомления] Уведомление записано для пользователя {}", event.getReceiverId());
        pushToUser(event.getReceiverId(), notificationMapper.toDto(saved));
    }

    private static boolean isLikeType(NotificationType type) {
        return type == NotificationType.POST_LIKE || type == NotificationType.PHOTO_LIKE;
    }

    private static boolean isOncePerTargetType(NotificationType type) {
        return type == NotificationType.EVENT_REMIND_1H
                || type == NotificationType.EVENT_REPUTATION_OPEN
                || type == NotificationType.EVENT_CHAT_KEEP_VOTE
                || type == NotificationType.EVENT_CANCELLED;
    }

    private static boolean isCollapsibleIdentityType(NotificationType type) {
        return type == NotificationType.EVENT_JOIN_REQUEST
                || type == NotificationType.EVENT_JOIN_SUCCESS
                || type == NotificationType.EVENT_JOIN_REJECTED
                || type == NotificationType.EVENT_JOIN_BANNED
                || type == NotificationType.EVENT_KICK
                || type == NotificationType.FRIEND_REQUEST_SENT
                || type == NotificationType.FRIEND_REQUEST_ACCEPTED;
    }

    private void pushToUser(Long receiverId, NotificationDto dto) {
        try {
            messagingTemplate.convertAndSend(
                    NotificationConstants.WS_TOPIC_PREFIX + receiverId,
                    dto
            );
            appMetrics.uvedomlenieOtpravleno();
            webPushService.sendToUser(receiverId, dto);
        } catch (Exception ex) {
            appMetrics.uvedomlenieOshibka();
            log.error("[Уведомления] Не удалось отправить в сокет пользователю {}: {}", receiverId, ex.getMessage());
        }
    }

    private void sendLikeSocketSync(NotificationEvent event, String message) {
        NotificationDto dto = new NotificationDto();
        dto.setTargetId(event.getTargetId());
        dto.setSenderId(event.getSenderId());
        dto.setType(event.getType());
        dto.setMessage(message);
        messagingTemplate.convertAndSend(
                NotificationConstants.WS_TOPIC_PREFIX + event.getReceiverId(),
                dto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationGroupDto> getUserNotifications(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        List<NotificationEntity> entities = repository
                .findAllByReceiverIdAndTypeNot(userId, NotificationType.NEW_CHAT_MESSAGE, pageable)
                .getContent();
        return notificationAggregator.aggregateNotifications(entities);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = repository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Уведомление не найдено"));
        if (!notification.getReceiverId().equals(userId)) {
            throw new NotificationAccessDeniedException("Вы не можете изменять чужие уведомления");

        }
        notification.setRead(true);
    }

    @Override
    @Transactional
    public void markGroupAsRead(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) return;
        List<Long> capped = ids.size() > 200 ? ids.subList(0, 200) : ids;
        log.info("[NotificationService] Массовая пометка прочитанными {} уведомлений для юзера {}", capped.size(), userId);
        repository.markIdsAsRead(capped, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        repository.markAllUnreadAsRead(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        log.info("[NotificationService] Подсчет непрочитанных системных уведомлений (без чатов) для юзера: {}", userId);
        // Считаем всё, кроме сообщений из чата
        return repository.countByReceiverIdAndReadFalseAndTypeNot(userId, NotificationType.NEW_CHAT_MESSAGE);
    }

    @Override
    @Transactional
    public void updateLocalUserSettings(UserSettingsUpdatedEvent event) {
        log.info("[NotificationService] Получено обновление настроек для юзера {}", event.getUserId());

        UserSettingsDto dto = event.getSettings();
        if (dto == null) {
            log.warn("[NotificationService] Объект настроек в событии пуст. Отмена сохранения.");
            return;
        }

        NotificationSettingsEntity settings = settingsRepository.findById(event.getUserId())
                .orElseGet(
                        () -> NotificationSettingsEntity.builder()
                                .userId(event.getUserId())
                                .build()
                );

        // Извлекаем только те четыре тумблера, которые нужны сервису уведомлений
        if (dto.getNotifyComments() != null) settings.setNotifyComments(dto.getNotifyComments());
        if (dto.getNotifyMessages() != null) settings.setNotifyMessages(dto.getNotifyMessages());
        if (dto.getNotifyEventRequests() != null) settings.setNotifyEventRequests(dto.getNotifyEventRequests());
        if (dto.getNotifyReputation() != null) settings.setNotifyReputation(dto.getNotifyReputation());

        settingsRepository.save(settings);
        log.info("[NotificationService - SUCCESS] Настройки из DTO успешно закешированы в локальную БД");
    }

    @Override
    @Transactional
    public void initDefaultSettingsForNewUser(Long userId) {
        log.info("[NotificationService] Инициализация дефолтных настроек уведомлений для нового пользователя {}", userId);

        // Проверяем на всякий случай, чтобы случайно не перезаписать, если запись уже есть
        if (!settingsRepository.existsById(userId)) {
            NotificationSettingsEntity defaultSettings = NotificationSettingsEntity.builder()
                    .userId(userId)
                    .notifyComments(true)
                    .notifyMessages(true)
                    .notifyEventRequests(true)
                    .notifyReputation(true)
                    .build();

            settingsRepository.save(defaultSettings);
            log.info("[NotificationService - SUCCESS] База данных уведомлений успешно синхронизирована для нового пользователя {}", userId);
        }
    }

    private boolean checkRemoteUserSettings(Long receiverId, NotificationType type) {
        try {
            // Для системных алертов, исключений и успешных вступлений БД не трогаем — всегда true
            if (type == NotificationType.EVENT_KICK || type == NotificationType.EVENT_JOIN_SUCCESS
                    || type == NotificationType.EVENT_JOIN_REJECTED || type == NotificationType.EVENT_JOIN_BANNED
                    || type == NotificationType.EVENT_REMIND_1H || type == NotificationType.EVENT_CANCELLED
                    || type == NotificationType.EVENT_CHAT_KEEP_VOTE
                    || type == NotificationType.ADMIN_CONTENT_REMOVED
            ) {
                return true;
            }
            // Мгновенно вычитываем настройки из локальной БД модуля уведомлений
            return settingsRepository.findById(receiverId)
                    .map(settings -> switch (type) {
                        case NEW_COMMENT, COMMENT_REPLY, POST_LIKE, PHOTO_LIKE -> Boolean.TRUE.equals(settings.getNotifyComments());
                        case NEW_CHAT_MESSAGE -> Boolean.TRUE.equals(settings.getNotifyMessages());
                        case EVENT_JOIN_REQUEST -> Boolean.TRUE.equals(settings.getNotifyEventRequests());
                        case REPUTATION_UPDATE, EVENT_REPUTATION_OPEN -> Boolean.TRUE.equals(settings.getNotifyReputation());
                        default -> true; // Системные алерты (заявки в друзья) всегда включены
                    })
                    .orElse(true); // Если записей для юзера еще нет — по умолчанию всё включено!
        } catch (Exception e) {
            log.error("[NotificationService] Ошибка локальной проверки настроек для юзера {}. Фолбэк на true.", receiverId, e);
            return true;
        }
    }
}