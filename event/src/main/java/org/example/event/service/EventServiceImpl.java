package org.example.event.service;

import com.example.common.dto.event.EventDto;
import com.example.common.dto.event.EventPersonPreviewDto;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.EventCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.dto.CreateEventDto;
import org.example.event.dto.MyEventTabCountersDto;
import org.example.event.dto.NeighborEventCountersDto;
import org.example.event.entity.*;
import org.example.event.entity.enums.EventCategory;
import org.example.event.entity.enums.EventUserFilter;
import org.example.event.entity.enums.NeighborEventFilter;
import org.example.event.entity.enums.ParticipantStatus;
import org.example.event.exception.EventAccessDeniedException;
import org.example.event.exception.EventNotFoundException;
import org.example.event.mapper.EventMapper;
import org.example.event.config.KafkaTopics;
import org.example.event.outbox.OutboxService;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.repository.EventRepository;
import org.example.event.restClient.ChatRestClient;
import org.example.event.restClient.SchedulerRestClient;
import org.example.event.restClient.UserRestClient;
import com.example.common.metrics.AppMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService {

    private final EventParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final ChatRestClient chatRestClient;
    private final SchedulerRestClient schedulerRestClient;
    private final UserRestClient userRestClient;
    private final EventLifecycleService eventLifecycleService;
    private final OutboxService outboxService;
    private final AppMetrics appMetrics;

    @Override
    @Transactional
    public EventDto createEvent(CreateEventDto dto, Long organizerId) {
        log.info("[EventService - INFO] Создание нового события '{}' организатором: {}", dto.getTitle(), organizerId);
        EventEntity entity = eventMapper.toEntity(dto);
        entity.setOrganizerId(organizerId);
        entity.setCurrentParticipants(1); // Организатор — первый участник
        entity.setPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
        entity.setPhotos(normalizePhotos(dto.getPhotos()));
        EventEntity savedEntity = eventRepository.save(entity);
        appMetrics.vstrechaSozdana();
        log.info("[Встречи] Создана встреча id={} организатор={}", savedEntity.getId(), organizerId);

        EventCreatedEvent eventPayload =
                new EventCreatedEvent(savedEntity.getId(), savedEntity.getTitle(), organizerId);
        outboxService.enqueue(KafkaTopics.EVENT_CREATED, savedEntity.getId(), eventPayload);
        log.info("[Outbox] Создание встречи ID: {} записано в очередь", savedEntity.getId());

        afterCommit(() -> schedulerRestClient.scheduleLifecycle(EventLifecycleService.toRequest(savedEntity)));
        return toAccurateDto(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(Long id, Long viewerId) {
        log.info("[EventService - INFO] Получение события по id: {}", id);
        EventEntity entity = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return enrichDtos(List.of(entity), viewerId).get(0);
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long id, CreateEventDto updateEventDto, Long currentUserId) {
        log.info("[EventService - INFO] Обновление события с id: {} от пользователя: {}", id, currentUserId);
        EventEntity eventEntity = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (!eventEntity.getOrganizerId().equals(currentUserId)) {
            throw new EventAccessDeniedException("У вас нет прав на изменение");
        }

        LocalDateTime previousDate = eventEntity.getEventDate();
        updateEventFields(eventEntity, updateEventDto);
        EventEntity saved = eventRepository.save(eventEntity);
        boolean dateChanged = !Objects.equals(previousDate, saved.getEventDate());
        afterCommit(() -> {
            if (dateChanged) {
                schedulerRestClient.resetLifecycle(EventLifecycleService.toRequest(saved));
            } else {
                schedulerRestClient.scheduleLifecycle(EventLifecycleService.toRequest(saved));
            }
        });
        return toAccurateDto(saved);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id, Long currentUserId) {
        log.info("[EventService - INFO] Инициализация отмены события с id: {} организатором: {}", id, currentUserId);

        EventEntity entity = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (!entity.getOrganizerId().equals(currentUserId)) {
            throw new EventAccessDeniedException("У вас нет прав на удаление");
        }

        if (entity.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            // Вычисляем дедлайн отмены (время проведения встречи минус 12 часов)
            LocalDateTime cancelDeadline = entity.getEventDate().minusHours(12);

            if (now.isAfter(cancelDeadline)) {
                log.warn("[EventService - WARN] Попытка отмены встречи {} заблокирована. До мероприятия осталось менее 12 часов.", id);
                throw new IllegalArgumentException("Нельзя отменить встречу менее чем за 12 часов до её начала.");
            }
        }

        schedulerRestClient.cancelLifecycle(id);
        eventLifecycleService.notifyParticipants(id, com.example.common.kafka.NotificationType.EVENT_CANCELLED);

        // Сначала чат (fail-closed), затем БД — иначе orphan room
        chatRestClient.cancelChatRoom(id, currentUserId);

        participantRepository.deleteAllByEventId(id);
        log.info("[EventService] Все заявки и участники для события {} успешно зачищены из базы данных событий", id);

        eventRepository.delete(entity);
        log.info("[EventService - SUCCESS] Событие {} полностью удалено организатором", id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEvents(
            EventCategory category,
            BigDecimal minLat, BigDecimal maxLat,
            BigDecimal minLng, BigDecimal maxLng,
            Long viewerId
    ) {
        log.info("[EventService - INFO] Запрос списка событий на карту");
        try {
            List<EventEntity> entities;
            boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;

            if (hasBounds) {
                entities = (category != null)
                        ? eventRepository.findEventsInBoundsAndCategory(category, minLat, maxLat, minLng, maxLng)
                        : eventRepository.findEventsInBounds(minLat, maxLat, minLng, maxLng);
            } else {
                entities = (category != null) ? eventRepository.findAllByCategory(category) : eventRepository.findAll();
            }

            LocalDateTime now = LocalDateTime.now();

            List<EventEntity> upcoming = entities.stream()
                    .filter(entity -> entity.getEventDate() != null && !entity.getEventDate().isBefore(now))
                    .sorted(Comparator.comparing(EventEntity::getEventDate))
                    .toList();
            return enrichDtos(upcoming, viewerId);
        } catch (Exception ex) {
            appMetrics.vstrechaSpisokOshibka();
            log.error("[Встречи] Ошибка загрузки списка встреч на карте: {}", ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByEnumFilter(Long userId, EventUserFilter filter) {
        LocalDateTime now = LocalDateTime.now();
        List<EventEntity> entities;

        // Вытаскиваем из базы реальные ID событий, в которых юзер успешно участвует
        List<Long> joinedEventIds = participantRepository.findEventIdsByUserIdAndStatus(userId, ParticipantStatus.JOINED);
        boolean hasJoinedEvents = joinedEventIds != null && !joinedEventIds.isEmpty();

        switch (filter) {
            case ALL_UPCOMING:
                // Будущие встречи: передаем ID, но даже если вступал в 0 встреч, метод репозитория все равно вытащит созданные мной!
                entities = eventRepository.findUpcomingEventsAsParticipantOnly(
                        userId,
                        now,
                        joinedEventIds != null ? joinedEventIds : java.util.Collections.emptyList()
                );
                break;

            case ALL_PAST:
                // Прошедшие: События в прошлом, где я создатель ИЛИ одобренный участник
                entities = hasJoinedEvents
                        ? eventRepository.findPastUserActivity(userId, now, joinedEventIds)
                        : eventRepository.findPastEventsAsOrganizer(userId, now);
                break;

            case MY_UPCOMING:
                // Мои события -> Будущие: я строго Создатель
                entities = eventRepository.findUpcomingEventsAsOrganizer(userId, now);
                break;

            case MY_PAST:
                // Мои события -> Прошедшие: я строго Создатель
                entities = eventRepository.findPastEventsAsOrganizer(userId, now);
                break;

            default:
                entities = java.util.Collections.emptyList();
        }

        List<EventEntity> sorted = entities.stream()
                .sorted((e1, e2) -> {
                    if (e1.getEventDate() == null) return 1;
                    if (e2.getEventDate() == null) return -1;

                    if (filter == EventUserFilter.ALL_PAST || filter == EventUserFilter.MY_PAST) {
                        return e2.getEventDate().compareTo(e1.getEventDate());
                    }
                    return e1.getEventDate().compareTo(e2.getEventDate());
                })
                .toList();
        return enrichDtos(sorted, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getPublicEventsByNeighborId(Long neighborId, NeighborEventFilter filter, Long viewerId) {
        LocalDateTime now = LocalDateTime.now();
        List<EventEntity> events;

        if (filter == NeighborEventFilter.USER_CREATED) {
            events = eventRepository.findByOrganizerId(neighborId);
            events = events.stream()
                    .sorted((e1, e2) -> compareNeighborEvents(e1, e2, now, false))
                    .toList();
        } else if (filter == NeighborEventFilter.USER_PAST) {
            List<Long> joinedEventIds = participantRepository.findEventIdsByUserIdAndStatus(
                    neighborId, ParticipantStatus.JOINED);
            boolean hasJoinedEvents = joinedEventIds != null && !joinedEventIds.isEmpty();
            events = hasJoinedEvents
                    ? eventRepository.findPastUserActivity(neighborId, now, joinedEventIds)
                    : eventRepository.findPastEventsAsOrganizer(neighborId, now);
            events = events.stream()
                    .sorted((e1, e2) -> compareNeighborEvents(e1, e2, now, true))
                    .toList();
        } else if (filter == NeighborEventFilter.USER_UPCOMING) {
            events = eventRepository.findAllByOrganizerIdAndEventDateAfterOrderByEventDateAsc(neighborId, now);
        } else {
            events = eventRepository.findByOrganizerId(neighborId);
        }

        return enrichDtos(events, viewerId);
    }

    private int compareNeighborEvents(EventEntity e1, EventEntity e2, LocalDateTime now, boolean pastOnly) {
        LocalDateTime d1 = e1.getEventDate();
        LocalDateTime d2 = e2.getEventDate();
        if (d1 == null) return 1;
        if (d2 == null) return -1;
        if (pastOnly) {
            return d2.compareTo(d1);
        }
        boolean past1 = !d1.isAfter(now);
        boolean past2 = !d2.isAfter(now);
        if (past1 != past2) {
            return past1 ? 1 : -1;
        }
        return past1 ? d2.compareTo(d1) : d1.compareTo(d2);
    }

    /** Счётчик = организатор + JOINED (не доверяем устаревшему currentParticipants в БД). */
    private EventDto toAccurateDto(EventEntity entity) {
        return enrichDtos(List.of(entity), null).get(0);
    }

    private static final int PEOPLE_PREVIEW_LIMIT = 3;

    private List<EventDto> enrichDtos(List<EventEntity> entities, Long viewerId) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = entities.stream().map(EventEntity::getId).filter(Objects::nonNull).toList();

        Map<Long, Long> joinedCounts = new HashMap<>();
        if (!eventIds.isEmpty()) {
            for (Object[] row : participantRepository.countJoinedByEventIds(eventIds, ParticipantStatus.JOINED)) {
                if (row == null || row[0] == null) {
                    continue;
                }
                joinedCounts.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        Map<Long, ParticipantStatus> viewerStatus = new HashMap<>();
        if (viewerId != null && !eventIds.isEmpty()) {
            for (EventParticipantEntity rel : participantRepository.findByEventIdInAndUserId(eventIds, viewerId)) {
                viewerStatus.put(rel.getEventId(), rel.getStatus());
            }
        }

        List<EventParticipantEntity> joinedPeople = eventIds.isEmpty()
                ? List.of()
                : participantRepository.findByEventIdInAndStatus(eventIds, ParticipantStatus.JOINED);

        Map<Long, List<Long>> previewIdsByEvent = new HashMap<>();
        Set<Long> userIdsToFetch = new HashSet<>();
        for (EventEntity entity : entities) {
            List<Long> preview = new ArrayList<>();
            if (entity.getOrganizerId() != null) {
                preview.add(entity.getOrganizerId());
                userIdsToFetch.add(entity.getOrganizerId());
            }
            previewIdsByEvent.put(entity.getId(), preview);
        }
        for (EventParticipantEntity participant : joinedPeople) {
            List<Long> preview = previewIdsByEvent.get(participant.getEventId());
            if (preview == null || participant.getUserId() == null) {
                continue;
            }
            if (preview.contains(participant.getUserId()) || preview.size() >= PEOPLE_PREVIEW_LIMIT) {
                continue;
            }
            preview.add(participant.getUserId());
            userIdsToFetch.add(participant.getUserId());
        }

        Map<Long, UserDto> usersMap = userRestClient.getUsersByIds(new ArrayList<>(userIdsToFetch)).stream()
                .filter(user -> user != null && user.getUserId() != null)
                .collect(Collectors.toMap(UserDto::getUserId, Function.identity(), (a, b) -> a));

        List<EventDto> result = new ArrayList<>(entities.size());
        for (EventEntity entity : entities) {
            EventDto dto = eventMapper.toDto(entity);
            long joined = joinedCounts.getOrDefault(entity.getId(), 0L);
            dto.setCurrentParticipants(1 + (int) joined);
            dto.setCanVoteReputation(eventLifecycleService.canVoteReputation(entity));
            if (entity.getEventDate() != null) {
                dto.setReputationOpensAt(eventLifecycleService.reputationOpensAt(entity));
                dto.setReputationClosesAt(eventLifecycleService.reputationClosesAt(entity));
            }
            UserDto organizer = usersMap.get(entity.getOrganizerId());
            if (organizer != null) {
                String first = organizer.getFirstName() != null ? organizer.getFirstName() : "Сосед";
                String last = organizer.getLastName() != null ? organizer.getLastName() : "";
                dto.setOrganizerName((first + " " + last).trim());
                dto.setOrganizerAvatarUrl(organizer.getAvatarUrl());
            } else {
                dto.setOrganizerName("Организатор");
            }
            if (viewerId != null) {
                if (entity.getOrganizerId() != null && entity.getOrganizerId().equals(viewerId)) {
                    dto.setUserStatus("JOINED");
                } else {
                    ParticipantStatus status = viewerStatus.get(entity.getId());
                    dto.setUserStatus(status != null ? status.name() : "NONE");
                }
            }
            List<EventPersonPreviewDto> people = new ArrayList<>();
            for (Long uid : previewIdsByEvent.getOrDefault(entity.getId(), List.of())) {
                UserDto user = usersMap.get(uid);
                EventPersonPreviewDto person = new EventPersonPreviewDto();
                person.setUserId(uid);
                if (user != null) {
                    person.setFirstName(user.getFirstName() != null ? user.getFirstName() : "Сосед");
                    person.setLastName(user.getLastName() != null ? user.getLastName() : "");
                    person.setAvatarUrl(user.getAvatarUrl());
                } else {
                    person.setFirstName("Сосед");
                    person.setLastName("#" + uid);
                }
                people.add(person);
            }
            dto.setPeoplePreview(people);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MyEventTabCountersDto getMyTabCounters(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> joined = participantRepository.findEventIdsByUserIdAndStatus(userId, ParticipantStatus.JOINED);
        boolean hasJoined = joined != null && !joined.isEmpty();

        int upcoming;
        int past;
        if (hasJoined) {
            upcoming = (int) eventRepository.countUpcomingEventsAsParticipantOnly(userId, now, joined);
            past = (int) eventRepository.countPastUserActivity(userId, now, joined);
        } else {
            upcoming = eventRepository.countByOrganizerIdAndEventDateAfter(userId, now);
            past = eventRepository.countByOrganizerIdAndEventDateBefore(userId, now);
        }
        int mineUpcoming = eventRepository.countByOrganizerIdAndEventDateAfter(userId, now);
        int minePast = eventRepository.countByOrganizerIdAndEventDateBefore(userId, now);
        return new MyEventTabCountersDto(upcoming, past, mineUpcoming, minePast);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public NeighborEventCountersDto getEventCountersByUserId(Long neighborId) {
        LocalDateTime now = LocalDateTime.now();

        int totalCreatedCount = eventRepository.countByOrganizerId(neighborId);
        int ownPastEventsCount = eventRepository.countByOrganizerIdAndEventDateBefore(neighborId, now);
        int foreignAttendedPastCount = participantRepository.countAttendedEvents(
                neighborId,
                ParticipantStatus.JOINED,
                now
        );
        int totalVisitedCount = ownPastEventsCount + foreignAttendedPastCount;

        return new NeighborEventCountersDto(totalVisitedCount, totalCreatedCount);
    }


    private void updateEventFields(EventEntity entity, CreateEventDto dto) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        entity.setPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setLocationName(dto.getLocationName());
        entity.setEventDate(dto.getEventDate());
        entity.setParticipantLimit(dto.getParticipantLimit());
        entity.setTags(dto.getTags());
        entity.setPhotos(normalizePhotos(dto.getPhotos()));
    }

    private List<String> normalizePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<String> cleaned = photos.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .limit(5)
                .collect(Collectors.toList());
        return cleaned;
    }

}