package org.example.event.service;

import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.UserReputationChangedEvent;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.event.config.KafkaTopics;
import org.example.event.entity.*;
import org.example.event.entity.enums.ParticipantStatus;
import org.example.event.entity.enums.VoteType;
import org.example.event.exception.EventAccessDeniedException;
import org.example.event.exception.EventNotFoundException;
import org.example.event.exception.EventValidationException;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.repository.EventRepository;
import org.example.event.repository.EventUserReputationVoteRepository;
import org.example.event.outbox.OutboxService;
import com.example.common.lifecycle.EventLifecycleSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventUserReputationVoteServiceImpl implements EventUserReputationVoteService {

    private final EventUserReputationVoteRepository voteRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final OutboxService outboxService;
    private final NotificationKafkaProducer notificationProducer;
    private final EventLifecycleSettings lifecycleSettings;

    @Override
    @Transactional
    public void vote(Long eventId, Long voterId, Long targetId, VoteType voteType) {
        log.info("[Reputation-Service] Запрос на оценку от юзера {} к юзеру {} за событие {}", voterId, targetId, eventId);

        if (voterId.equals(targetId)) {
            throw new EventValidationException("Вы не можете оценивать самого себя");
        }

        // 1. Проверяем, существует ли событие через кастомное исключение
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. Валидируем правила
        validateVoteConditions(event, voterId, targetId);

        // 3. ЛОГИКА СЧЕТЧИКА И АНТИ-СПАМА УВЕДОМЛЕНИЙ
        Optional<EventUserReputationVoteEntity> existingVoteOpt = voteRepository.findByEventIdAndVoterIdAndTargetId(eventId, voterId, targetId);
        Long changeValue = 0L;
        boolean shouldNotify = false;
        VoteType actualVoteTypeToSend = voteType;

        if (existingVoteOpt.isPresent()) {
            EventUserReputationVoteEntity existingVote = existingVoteOpt.get();
            if (existingVote.getVoteType() == voteType) {
                // СИТУАЦИЯ А: Кликнули по той же кнопке повторно -> УДАЛЯЕМ голос (сброс оценки)
                voteRepository.delete(existingVote);
                log.info("[Reputation-Service] Повторный клик. Оценка удалена из БД");
                changeValue = (voteType == VoteType.PLUS) ? -1L : 1L;
                // При полном сбросе оценки уведомление пользователю не шлем
            } else {
                // СИТУАЦИЯ Б: Изменили мнение (нажали на противоположную кнопку) -> ОБНОВЛЯЕМ голос
                existingVote.setVoteType(voteType);

                // Инкрементируем счетчик отправленных уведомлений для этой пары пользователей
                existingVote.setNotificationCount(existingVote.getNotificationCount() + 1);

                // Проверяем лимит: шлем пуш только если это суммарно 2-я попытка (первая смена мнения)
                if (existingVote.getNotificationCount() <= 2) {
                    shouldNotify = true;
                } else {
                    log.info("[Anti-Spam] Лимит уведомлений превышен (Попытка {}). Пуш заблокирован.", existingVote.getNotificationCount());
                }

                voteRepository.save(existingVote);
                log.info("[Reputation-Service] Мнение изменено. Тип голоса обновлен в БД");
                changeValue = (voteType == VoteType.PLUS) ? 2L : -2L;
            }
        } else {
            // СИТУАЦИЯ В: Голоса еще не было в базе -> СОЗДАЕМ новую запись
            EventUserReputationVoteEntity voteEntity = EventUserReputationVoteEntity.builder()
                    .eventId(eventId)
                    .voterId(voterId)
                    .targetId(targetId)
                    .voteType(voteType)
                    .notificationCount(1) // Это ПЕРВАЯ отправка уведомления для этой связки
                    .build();

            voteRepository.save(voteEntity);
            log.info("[Reputation-Service] Новая оценка успешно сохранена в БД");
            changeValue = (voteType == VoteType.PLUS) ? 1L : -1L;
            shouldNotify = true; // Первая оценка гарантированно отправляет пуш
        }

        // 4. Отправка выверенной дельты в Kafka для User-Service
        if (changeValue != 0L) {
            outboxService.enqueue(
                    KafkaTopics.USER_REPUTATION,
                    targetId,
                    new UserReputationChangedEvent(targetId, changeValue)
            );
            log.info("[Outbox] Сигнал изменения репутации ({}) для юзера {} записан в очередь", changeValue, targetId);
        }

        // 5. АНОНИМНОЕ УВЕДОМЛЕНИЕ В KAFKA С ИСКЛЮЧЕНИЕМ СПАМА (ОГРАНИЧЕНИЕ 2 РАЗА)
        if (shouldNotify) {
            sendReputationNotification(eventId, targetId, event.getTitle(), voteType);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventUserReputationVoteEntity> getMyVotesInEvent(Long eventId, Long voterId) {
        return voteRepository.findAllByEventIdAndVoterId(eventId, voterId);
    }

    private void validateVoteConditions(EventEntity event, Long voterId, Long targetId) {
        if (voterId.equals(targetId)) {
            throw new EventValidationException("Вы не можете оценивать самого себя");
        }

        if (event.getEventDate() != null && event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new EventValidationException("Оценки можно ставить через "
                    + lifecycleSettings.minutesLabel(lifecycleSettings.getReputationOpenMinutes())
                    + " после начала встречи");
        }
        if (event.getEventDate() != null && !lifecycleSettings.isReputationOpen(event.getEventDate(), LocalDateTime.now())) {
            if (LocalDateTime.now().isBefore(lifecycleSettings.reputationOpensAt(event.getEventDate()))) {
                throw new EventValidationException("Оценки можно ставить через "
                        + lifecycleSettings.minutesLabel(lifecycleSettings.getReputationOpenMinutes())
                        + " после начала встречи");
            }
            throw new EventValidationException("Окно оценок за эту встречу уже закрыто");
        }

        boolean isVoterOrganizer = event.getOrganizerId() != null && event.getOrganizerId().equals(voterId);
        boolean isVoterJoined = participantRepository.findByEventIdAndUserId(event.getId(), voterId)
                .map(p -> p.getStatus() == ParticipantStatus.JOINED).orElse(false);

        if (!isVoterOrganizer && !isVoterJoined) {
            throw new EventAccessDeniedException("Оценивать участников могут только создатель встречи или одобренные пользователи");
        }

        boolean isTargetOrganizer = event.getOrganizerId() != null && event.getOrganizerId().equals(targetId);
        boolean isTargetJoinedParticipant = participantRepository.findByEventIdAndUserId(event.getId(), targetId)
                .map(p -> p.getStatus() == ParticipantStatus.JOINED).orElse(false);

        if (!isTargetOrganizer && !isTargetJoinedParticipant) {
            throw new EventAccessDeniedException("Оценивать друг друга могут только участники или организатор этой встречи");
        }
    }

    private void sendReputationNotification(Long eventId, Long targetId, String eventTitle, VoteType voteType) {
        String notificationText = (voteType == VoteType.PLUS)
                ? "Ваша репутация на районе была увеличена на 1 за встречу: «%s»".formatted(eventTitle)
                : "Ваша репутация на районе была уменьшена на 1 за встречу: «%s»".formatted(eventTitle);
        try {
            notificationProducer.sendEvent(
                    targetId,
                    0L,
                    "",
                    "",
                    com.example.common.kafka.NotificationType.REPUTATION_UPDATE,
                    eventId,
                    null,
                    notificationText
            );
            log.info("[Anti-Spam - SUCCESS] Анонимное уведомление улетело в топик. Лимит пропущен.");
        } catch (Exception e) {
            log.error("[Reputation-Service - ERROR] Ошибка отправки нотификации в брокер: {}", e.getMessage());
        }
    }
}