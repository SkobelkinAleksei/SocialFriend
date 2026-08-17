package org.example.event.service;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import com.example.common.dto.scheduler.EventLifecycleSnapshot;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import com.example.common.lifecycle.EventLifecycleSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.entity.EventEntity;
import org.example.event.entity.EventParticipantEntity;
import org.example.event.entity.enums.ParticipantStatus;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLifecycleService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final NotificationKafkaProducer notificationProducer;
    private final EventLifecycleSettings lifecycleSettings;

    public static EventLifecycleRequest toRequest(EventEntity event) {
        return EventLifecycleRequest.builder()
                .eventId(event.getId())
                .organizerId(event.getOrganizerId())
                .title(event.getTitle())
                .eventDate(event.getEventDate())
                .build();
    }

    @Transactional(readOnly = true)
    public EventLifecycleSnapshot snapshot(Long eventId) {
        return eventRepository.findById(eventId)
                .map(this::toSnapshot)
                .orElseGet(() -> EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build());
    }

    @Transactional
    public EventLifecycleSnapshot markStarted(Long eventId) {
        EventEntity event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return EventLifecycleSnapshot.builder().exists(false).eventId(eventId).build();
        }
        autoRejectPending(event);
        return toSnapshot(event);
    }

    @Transactional(readOnly = true)
    public void notifyParticipants(Long eventId, NotificationType type) {
        EventEntity event = eventRepository.findById(eventId).orElse(null);
        if (event == null || type == null) {
            return;
        }
        String message = switch (type) {
            case EVENT_REMIND_1H -> "Встреча «%s» начнётся через %s".formatted(
                    event.getTitle(), lifecycleSettings.minutesLabel(lifecycleSettings.getRemindMinutesBefore()));
            case EVENT_REPUTATION_OPEN -> "Можно поставить оценку участникам встречи «%s»".formatted(event.getTitle());
            case EVENT_CANCELLED -> "Встреча «%s» отменена. Чат будет удалён через %s".formatted(
                    event.getTitle(), lifecycleSettings.minutesLabel(lifecycleSettings.getCancelDeleteMinutes()));
            case EVENT_CHAT_KEEP_VOTE -> "Проголосуйте, оставлять ли чат встречи «%s»".formatted(event.getTitle());
            default -> event.getTitle();
        };
        for (Long userId : eligibleVoterIds(event)) {
            try {
                notificationProducer.sendEvent(
                        userId,
                        0L,
                        "",
                        "",
                        type,
                        event.getId(),
                        null,
                        message
                );
            } catch (Exception e) {
                log.error("[EventLifecycle] Не удалось отправить {} пользователю {}: {}", type, userId, e.getMessage());
            }
        }
    }

    private void autoRejectPending(EventEntity event) {
        List<EventParticipantEntity> hanging = new ArrayList<>();
        hanging.addAll(participantRepository.findAllByEventIdAndStatus(event.getId(), ParticipantStatus.PENDING));
        hanging.addAll(participantRepository.findAllByEventIdAndStatus(event.getId(), ParticipantStatus.RE_PENDING));
        if (hanging.isEmpty()) {
            return;
        }
        String text = "Встреча «%s» уже началась, заявка отклонена автоматически".formatted(event.getTitle());
        for (EventParticipantEntity participant : hanging) {
            participant.setStatus(ParticipantStatus.REJECTED);
            participantRepository.save(participant);
            try {
                notificationProducer.sendEvent(
                        participant.getUserId(),
                        0L,
                        "",
                        "",
                        NotificationType.EVENT_JOIN_REJECTED,
                        event.getId(),
                        null,
                        text
                );
            } catch (Exception e) {
                log.error("[EventLifecycle] Не удалось уведомить об автоотклонении {}: {}",
                        participant.getUserId(), e.getMessage());
            }
        }
        log.info("[EventLifecycle] Автоотклонено {} заявок на встречу {}", hanging.size(), event.getId());
    }

    private EventLifecycleSnapshot toSnapshot(EventEntity event) {
        List<Long> eligible = eligibleVoterIds(event);
        LocalDateTime now = LocalDateTime.now();
        boolean started = event.getEventDate() != null && !event.getEventDate().isAfter(now);
        return EventLifecycleSnapshot.builder()
                .exists(true)
                .eventId(event.getId())
                .organizerId(event.getOrganizerId())
                .title(event.getTitle())
                .eventDate(event.getEventDate())
                .started(started)
                .peopleCount(eligible.size())
                .eligibleVoterIds(eligible)
                .build();
    }

    private List<Long> eligibleVoterIds(EventEntity event) {
        Set<Long> ids = new LinkedHashSet<>();
        if (event.getOrganizerId() != null) {
            ids.add(event.getOrganizerId());
        }
        participantRepository.findAllByEventIdAndStatus(event.getId(), ParticipantStatus.JOINED)
                .stream()
                .map(EventParticipantEntity::getUserId)
                .forEach(ids::add);
        return new ArrayList<>(ids);
    }

    public boolean canVoteReputation(EventEntity event) {
        return lifecycleSettings.isReputationOpen(event.getEventDate(), LocalDateTime.now());
    }

    public LocalDateTime reputationOpensAt(EventEntity event) {
        return event.getEventDate() == null ? null : lifecycleSettings.reputationOpensAt(event.getEventDate());
    }

    public LocalDateTime reputationClosesAt(EventEntity event) {
        return event.getEventDate() == null ? null : lifecycleSettings.reputationClosesAt(event.getEventDate());
    }
}
