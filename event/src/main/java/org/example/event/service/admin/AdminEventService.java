package org.example.event.service.admin;

import com.example.common.dto.event.EventDto;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.entity.EventEntity;
import org.example.event.entity.EventParticipantEntity;
import org.example.event.entity.enums.ParticipantStatus;
import org.example.event.exception.EventNotFoundException;
import org.example.event.mapper.EventMapper;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.repository.EventRepository;
import org.example.event.restClient.ChatRestClient;
import org.example.event.restClient.SchedulerRestClient;
import org.example.event.service.EventLifecycleService;
import org.example.event.service.EventParticipantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EventMapper eventMapper;
    private final SchedulerRestClient schedulerRestClient;
    private final EventLifecycleService eventLifecycleService;
    private final ChatRestClient chatRestClient;
    private final EventParticipantService eventParticipantService;

    @Transactional(readOnly = true)
    public EventDto preview(Long eventId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return eventMapper.toDto(event);
    }

    @Transactional
    public void cancelAsModerator(Long eventId) {
        EventEntity entity = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        doCancel(entity);
    }

    @Transactional
    public void applyBan(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<EventEntity> upcoming = eventRepository.findUpcomingEventsAsOrganizer(userId, now);
        Set<Long> cancelledIds = new HashSet<>();
        for (EventEntity event : upcoming) {
            try {
                doCancel(event);
                cancelledIds.add(event.getId());
            } catch (Exception e) {
                log.warn("[Admin] Не удалось отменить встречу {} организатора {}: {}",
                        event.getId(), userId, e.getMessage());
            }
        }
        List<EventParticipantEntity> parts = participantRepository.findAllByUserId(userId);
        for (EventParticipantEntity part : parts) {
            if (cancelledIds.contains(part.getEventId())) {
                continue;
            }
            ParticipantStatus status = part.getStatus();
            if (status != ParticipantStatus.JOINED
                    && status != ParticipantStatus.PENDING
                    && status != ParticipantStatus.RE_PENDING) {
                continue;
            }
            try {
                eventParticipantService.leaveEvent(part.getEventId(), userId);
            } catch (Exception e) {
                log.info("[Admin] Выход из встречи {} для {} пропущен: {}",
                        part.getEventId(), userId, e.getMessage());
            }
        }
    }

    private void doCancel(EventEntity entity) {
        Long id = entity.getId();
        schedulerRestClient.cancelLifecycle(id);
        eventLifecycleService.notifyParticipants(id, NotificationType.EVENT_CANCELLED);
        chatRestClient.cancelChatRoom(id, entity.getOrganizerId());
        participantRepository.deleteAllByEventId(id);
        eventRepository.delete(entity);
        log.info("[Admin] Встреча {} отменена модерацией", id);
    }
}
