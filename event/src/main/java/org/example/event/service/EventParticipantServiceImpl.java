package org.example.event.service;

import com.example.common.dto.event.EventDto;
import com.example.common.dto.event.IncomingRequestDto;
import com.example.common.dto.event.OutgoingRequestDto;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.EventUserJoinedEvent;
import com.example.common.kafka.EventUserLeftEvent;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.config.KafkaTopics;
import org.example.event.dto.ParticipantUserDto;
import org.example.event.entity.EventEntity;
import org.example.event.entity.EventParticipantEntity;
import org.example.event.entity.enums.ParticipantStatus;
import org.example.event.exception.EventAccessDeniedException;
import org.example.event.exception.EventNotFoundException;
import org.example.event.exception.ParticipantLimitExceededException;
import org.example.event.exception.RequestNotFoundException;
import org.example.event.exception.UserRestClientException;
import org.example.event.mapper.EventMapper;
import org.example.event.outbox.OutboxService;
import org.example.event.repository.EventRepository;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.restClient.UserRestClient;
import com.example.common.metrics.AppMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventParticipantServiceImpl implements EventParticipantService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EventMapper eventMapper;
    private final OutboxService outboxService;
    private final NotificationKafkaProducer notificationProducer;
    private final UserRestClient userRestClient;
    private final EventLifecycleService eventLifecycleService;
    private final AppMetrics appMetrics;

    @Override
    @Transactional
    public EventDto joinEvent(Long eventId, Long userId) {
        log.info("[ParticipantService - INFO] Юзер {} вступает в открытое событие {}", userId, eventId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.isPrivate()) {
            throw new IllegalStateException("Это приватное событие. Используйте метод applyToEvent");
        }
        assertEventStillOpenForJoin(event);

        participantRepository.findByEventIdAndUserId(eventId, userId).ifPresent(p -> {
            if (p.getStatus() == ParticipantStatus.BANNED) {
                throw new IllegalStateException("Организатор встречи окончательно отклонил вашу кандидатуру");
            }
            if (p.getStatus() == ParticipantStatus.KICKED) {
                throw new IllegalStateException("Вас исключили из встречи. Вернуть может только организатор");
            }
            throw new IllegalStateException("Вы уже подали заявку или участвуете");
        });

        if (eventRepository.tryIncrementParticipants(eventId) == 0) {
            throw new ParticipantLimitExceededException();
        }

        try {
            participantRepository.save(EventParticipantEntity.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.JOINED)
                    .build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            eventRepository.tryDecrementParticipants(eventId);
            throw new IllegalStateException("Вы уже подали заявку или участвуете");
        }

        EventDto dto = toDtoWithSyncedCount(eventId);

        UserDto userDto = userRestClient.getUserById(userId)
                .orElseThrow(() -> new UserRestClientException("Не удалось получить данные пользователя из микросервиса user"));

        enqueueUserJoined(eventId, userId, userDto);
        appMetrics.vstrechaVstuplenie();
        log.info("[Встречи] Пользователь {} вступил в открытую встречу {}", userId, eventId);
        try {
            notificationProducer.sendEvent(
                    event.getOrganizerId(), // receiverId (Организатор)
                    userId, // senderId (Кто вступил)
                    userDto.getFirstName(),
                    userDto.getLastName(),
                    NotificationType.EVENT_JOIN_REQUEST,
                    event.getId(),
                    null,
                    "присоединился(ась) к вашей встрече: «%s»".formatted(event.getTitle()),
                    "OPEN_JOIN" // открытое событие → клик ведёт в групповой чат, не в заявки
            );
            log.info("[ParticipantService] Отправлено уведомление организатору о новом участнике встречи {}", eventId);
        } catch (Exception e) {
            log.error("[Notification - ERROR] Не удалось отправить уведомление о вступлении: {}", e.getMessage());
        }

        try {
            notificationProducer.sendEvent(
                    userId,
                    0L,
                    "",
                    "",
                    NotificationType.EVENT_JOIN_SUCCESS,
                    event.getId(),
                    null,
                    "Вас добавили в групповой чат встречи «%s»".formatted(event.getTitle()),
                    "JOINER_CHAT"
            );
            log.info("[ParticipantService] Успешно отправлен EVENT_JOIN_SUCCESS для открытой встречи {}", eventId);
        } catch (Exception e) {
            log.error("[Notification - ERROR] Не удалось отправить уведомление о вступлении самому пользователю: {}", e.getMessage());
        }
        // ============================================================

        return dto;
    }


    @Override
    @Transactional
    public EventDto applyToEvent(Long eventId, Long userId) {
        log.info("[ParticipantService - INFO] Юзер {} подает заявку на приватное событие {}", userId, eventId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (!event.isPrivate()) {
            throw new IllegalStateException("Это открытое событие. Используйте метод joinEvent");
        }
        assertEventStillOpenForJoin(event);

        // Ищем существующую связь гостя с этим событием
        Optional<EventParticipantEntity> existingRelation = participantRepository.findByEventIdAndUserId(eventId, userId);

        // ЖЕЛЕЗНО ПРОВЕРЯЕМ ПРЕДЫСТОРИЮ (СТАРЫЙ СТАТУС):
        boolean isRepeatRequest = false;
        EventParticipantEntity relation;

        if (existingRelation.isPresent()) {
            relation = existingRelation.get();

            // Если это окончательный отказ (BANNED) — жесткий блок
            if (relation.getStatus() == ParticipantStatus.BANNED) {
                throw new IllegalStateException("Организатор встречи окончательно отклонил вашу кандидатуру");
            }

            // Кик из встречи — вернуться может только организатор
            if (relation.getStatus() == ParticipantStatus.KICKED) {
                throw new IllegalStateException("Вас исключили из встречи. Вернуть может только организатор");
            }

            if (relation.getStatus() == ParticipantStatus.JOINED) {
                throw new IllegalStateException("Вы уже участвуете в этой встрече");
            }

            // Если заявка уже висит на рассмотрении (PENDING или RE_PENDING) — не плодим дубликаты
            if (relation.getStatus() == ParticipantStatus.PENDING || relation.getStatus() == ParticipantStatus.RE_PENDING) {
                throw new IllegalStateException("Вы уже подали заявку или участвуете");
            }

            // Если это первый отказ -> фиксируем маркер ПОВТОРНОГО запроса и обновляем статус
            if (relation.getStatus() == ParticipantStatus.REJECTED) {
                relation.setStatus(ParticipantStatus.RE_PENDING);
                isRepeatRequest = true;
                log.info("[ParticipantService] Фиксация повторной заявки. Статус меняется на RE_PENDING");
            }
        } else {
            // Если пользователь стучится на это событие самый первый раз в жизни
            relation = EventParticipantEntity.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .status(ParticipantStatus.PENDING)
                    .build();
            log.info("[ParticipantService] Фиксация первой заявки. Статус устанавливается в PENDING");
        }

        // Сохраняем сущность связи в базу данных PostgreSQL
        participantRepository.save(relation);
        appMetrics.vstrechaZayavka();
        log.info("[Встречи] Заявка на встречу {} от пользователя {}", eventId, userId);

        // Запрашиваем ФИО подающего заявку соседа из User-Service для формирования пуша
        String applicantFirstName = "Житель";
        String applicantLastName = "";

        UserDto userDto = userRestClient.getUserById(userId).orElse(null);
        if (userDto != null) {
            applicantFirstName = userDto.getFirstName();
            applicantLastName = userDto.getLastName();
        }

        // Формируем умный динамический текст на основании нашего маркера
        String applyNotificationText;
        String applyContextLabel;
        if (isRepeatRequest) {
            applyNotificationText = "подал(а) повторную заявку на приватную встречу «%s»".formatted(event.getTitle());
            applyContextLabel = "PRIVATE_REAPPLY";
            log.info("[ParticipantService] Сформирован текст пуша для ПОВТОРНОЙ заявки от юзера {}", userId);
        } else {
            applyNotificationText = "подал(а) заявку на приватную встречу «%s»".formatted(event.getTitle());
            applyContextLabel = "PRIVATE_APPLY";
            log.info("[ParticipantService] Сформирован текст пуша для ПЕРВОЙ заявки от юзера {}", userId);
        }

        // Отправляем официальное уведомление организатору в Kafka топик
        try {
            notificationProducer.sendEvent(
                    event.getOrganizerId(),
                    userId,
                    applicantFirstName,
                    applicantLastName,
                    NotificationType.EVENT_JOIN_REQUEST,
                    event.getId(),
                    null,
                    applyNotificationText,
                    applyContextLabel
            );
            log.info("[ParticipantService] Уведомление успешно отправлено в шину событий для встречи {}", eventId);
        } catch (Exception e) {
            log.error("[Notification - ERROR] Не удалось отправить уведомление о заявке: {}", e.getMessage());
        }

        return eventMapper.toDto(event);
    }


    @Override
    @Transactional
    public void approveParticipant(Long eventId, Long participantId, Long organizerId) {
        log.info("[ParticipantService] Одобрение заявки по ID строки: {}", participantId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        assertOrganizer(event, organizerId);
        assertEventStillOpenForJoin(event);

        EventParticipantEntity participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RequestNotFoundException(participantId));

        if (!eventId.equals(participant.getEventId())) {
            throw new EventAccessDeniedException("Заявка не принадлежит указанному событию");
        }

        if (participant.getStatus() != ParticipantStatus.PENDING
                && participant.getStatus() != ParticipantStatus.RE_PENDING) {
            throw new IllegalStateException("Одобрить можно только заявку со статусом PENDING или RE_PENDING");
        }

        if (eventRepository.tryIncrementParticipants(eventId) == 0) {
            throw new ParticipantLimitExceededException();
        }

        participant.setStatus(ParticipantStatus.JOINED);
        participantRepository.save(participant);
        appMetrics.vstrechaVstuplenie();
        log.info("[Встречи] Заявка одобрена, пользователь вступил во встречу {}", eventId);
        syncCurrentParticipants(eventId);

        UserDto userDto = userRestClient.getUserById(participant.getUserId())
                .orElseThrow(() -> new UserRestClientException("Не удалось получить данные пользователя из микросервиса user"));

        enqueueUserJoined(eventId, participant.getUserId(), userDto);
        log.info("[ParticipantService - Outbox] Одобрение: пользователь {} будет добавлен в чат встречи {}", participant.getUserId(), eventId);

        try {
            notificationProducer.sendEvent(
                    participant.getUserId(),
                    organizerId,
                    "Организатор",
                    "",
                    NotificationType.EVENT_JOIN_SUCCESS,
                    event.getId(),
                    null,
                    "Вас добавили в групповой чат встречи «%s»".formatted(event.getTitle()),
                    "JOINER_CHAT"
            );
            log.info("[Notification - SUCCESS] Отправлен EVENT_JOIN_SUCCESS (одобрение) для юзера {}", participant.getUserId());
        } catch (Exception e) {
            log.error("[Notification - ERROR] Не удалось отправить уведомление об одобрении заявки: {}", e.getMessage());
        }
    }


    @Override
    @Transactional
    public void rejectParticipant(Long eventId, Long participantId, Long organizerId) {
        log.info("[ParticipantService] Инициализация исключения/отклонения по ID строки связи: {}", participantId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        assertOrganizer(event, organizerId);

        EventParticipantEntity participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RequestNotFoundException(participantId));

        if (!eventId.equals(participant.getEventId())) {
            throw new EventAccessDeniedException("Заявка не принадлежит указанному событию");
        }

        Long kickedUserId = participant.getUserId();
        ParticipantStatus oldStatus = participant.getStatus();

        String notificationText = "";
        NotificationType exactNotificationType = null;

        if (oldStatus == ParticipantStatus.JOINED) {
            participant.setStatus(ParticipantStatus.KICKED);
            log.info("[ParticipantService] Участник {} исключён (KICKED) из события {}", kickedUserId, eventId);

        } else if (oldStatus == ParticipantStatus.RE_PENDING) {
            // Второй отказ — навсегда
            participant.setStatus(ParticipantStatus.BANNED);
            exactNotificationType = NotificationType.EVENT_JOIN_BANNED;
            notificationText = "окончательно отклонил(а) вашу заявку на встречу: \"" + event.getTitle()
                    + "\". Повторная подача больше невозможна.";
            log.info("[ParticipantService] Повторный отказ → BANNED для юзера {}", kickedUserId);

        } else if (oldStatus == ParticipantStatus.PENDING) {
            // Первый отказ — можно подать снова
            participant.setStatus(ParticipantStatus.REJECTED);
            exactNotificationType = NotificationType.EVENT_JOIN_REJECTED;
            notificationText = "в первый раз отклонил(а) вашу заявку на встречу: \"" + event.getTitle()
                    + "\". Вы можете подать заявку ещё раз.";
            log.info("[ParticipantService] Первый отказ → REJECTED для юзера {}", kickedUserId);

        } else {
            throw new IllegalStateException("Нельзя отклонить участника со статусом " + oldStatus);
        }

        // Сначала коммит статуса в БД — иначе chat/notification уйдут при откате constraint
        participantRepository.saveAndFlush(participant);
        if (oldStatus == ParticipantStatus.JOINED) {
            syncCurrentParticipants(eventId);
        }

        final String eventTitle = event.getTitle();
        final Long eventIdFinal = event.getId();
        final NotificationType notifTypeFinal = exactNotificationType;
        final String notifTextFinal = notificationText;
        final ParticipantStatus oldStatusFinal = oldStatus;

        Runnable afterDbCommit = () -> {
            if (oldStatusFinal == ParticipantStatus.JOINED) {
                try {
                    String fName = "Организатор";
                    String lName = "";
                    UserDto organizerDto = userRestClient.getUserById(organizerId).orElse(null);
                    if (organizerDto != null) {
                        fName = organizerDto.getFirstName() != null ? organizerDto.getFirstName() : "Организатор";
                        lName = organizerDto.getLastName() != null ? organizerDto.getLastName() : "";
                    }
                    notificationProducer.sendEvent(
                            kickedUserId, organizerId, fName, lName,
                            NotificationType.EVENT_KICK, eventIdFinal, null,
                            "исключил(а) вас из события: \"" + eventTitle + "\""
                    );
                } catch (Exception e) {
                    log.error("[Notification - ERROR] Не удалось отправить уведомление о кике: {}", e.getMessage());
                }
                try {
                    sendUserLeftKafka(eventIdFinal, kickedUserId, ParticipantStatus.KICKED.name(), true);
                } catch (Exception e) {
                    log.error("[Kafka - ERROR] Кик сохранён в БД, но чат не синхронизирован: {}", e.getMessage());
                }
            } else if (notifTypeFinal != null) {
                try {
                    notificationProducer.sendEvent(
                            kickedUserId,
                            organizerId,
                            "Организатор",
                            "",
                            notifTypeFinal,
                            eventIdFinal,
                            null,
                            notifTextFinal
                    );
                } catch (Exception e) {
                    log.error("[Notification - ERROR] Не удалось отправить уведомление об отказе заявки: {}", e.getMessage());
                }
            }
        };

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            afterDbCommit.run();
                        }
                    }
            );
        } else {
            afterDbCommit.run();
        }
    }

    @Override
    @Transactional
    public EventDto leaveEvent(Long eventId, Long userId) {
        log.info("[ParticipantService - INFO] Юзер {} покидает событие или отзывает заявку {}", userId, eventId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        EventParticipantEntity participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("Вы не записаны"));

        // 1. ЖЕЛЕЗНАЯ ЗАЩИТА ОТ ГОНКИ: Если организатор уже успел забанить или отказать
        if (participant.getStatus() == ParticipantStatus.BANNED) {
            return eventMapper.toDto(event);
        }
        if (participant.getStatus() == ParticipantStatus.KICKED) {
            throw new IllegalStateException("Вас исключили из встречи. Вернуть может только организатор");
        }
        if (participant.getStatus() == ParticipantStatus.REJECTED) {
            throw new IllegalStateException("Организатор уже отклонил вашу заявку.");
        }

        // 2. ОТКАТ ДЛЯ ПОВТОРНЫХ ЗАЯВОК (АНТИ-ОБНУЛЕНИЕ)
        if (participant.getStatus() == ParticipantStatus.RE_PENDING) {
            participant.setStatus(ParticipantStatus.REJECTED);
            participantRepository.save(participant);
            log.info("[ParticipantService] Юзер {} отозвал повторную заявку. Статус успешно откачен назад на REJECTED", userId);

            return eventMapper.toDto(event);
        }

        // 3. СТАНДАРТНАЯ ЛОГИКА ДЛЯ ОДОБРЕННЫХ (JOINED) ИЛИ ПЕРВЫХ ЗАЯВОК (PENDING)
        if (participant.getStatus() == ParticipantStatus.JOINED) {
            if (event.getOrganizerId().equals(userId)) {
                throw new IllegalStateException("Организатор не может покинуть встречу, только удалить её");
            }
        }

        // Физически удаляем из базы только ПЕРВУЮ заявку (PENDING) или выход из открытой встречи (JOINED)
        participantRepository.delete(participant);
        log.info("[ParticipantService] Запись связи полностью удалена для юзера {}", userId);

        triggerKafkaLeftTopic(eventId, userId, "NONE");

        return toDtoWithSyncedCount(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserStatusForEvent(Long eventId, Long userId) {
        log.info("[ParticipantService] Запрос статуса юзера {} для встречи {}", userId, eventId);

        // Проверяем, не организатор ли он встречи априори
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getOrganizerId().equals(userId)) {
            return "JOINED"; // Организатор всегда имеет статус участника
        }

        // Ищем запись в таблице участников
        return participantRepository.findByEventIdAndUserId(eventId, userId)
                .map(participant -> participant.getStatus().name())
                .orElse("NONE"); // Если записи нет — статус по умолчанию NONE
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getUserParticipantsMap(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }

        List<EventParticipantEntity> participations = participantRepository.findAllByUserId(userId);

        return participations.stream().collect(Collectors.toMap(
                EventParticipantEntity::getEventId,
                p -> p.getStatus().name(),
                (existing, replacement) -> existing
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<IncomingRequestDto>> getIncomingRequestsForOrganizer(Long organizerId) {

        log.info("[ParticipantService] Сбор входящих заявок для организатора: {}", organizerId);
        // Шаг 1: Достаем из базы только те события, где текущий юзер является создателем
        List<EventEntity> myEvents = eventRepository.findByOrganizerId(organizerId).stream()
                .filter(e -> e.getEventDate() == null ||
                        e.getEventDate().isAfter(java.time.LocalDateTime.now()))
                .toList();

        // Если у пользователя нет созданных событий, возвращаем пустую карту
        if (myEvents.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> myEventIds = myEvents.stream()
                .map(EventEntity::getId)
                .toList();

        // Шаг 2: Ищем в репозитории участников ВСЕ заявки со статусом PENDING для этих событий
        List<ParticipantStatus> incomingStatuses = List.of(
                ParticipantStatus.PENDING,
                ParticipantStatus.RE_PENDING
        );

        List<EventParticipantEntity> pendingRelations = participantRepository.findByEventIdInAndStatusIn(
                myEventIds,
                incomingStatuses
        );

        // Шаг 3: Группируем заявки по eventId и обогащаем данными из внешнего User-сервиса (ПАКЕТНО)
        List<Long> applicantIds = pendingRelations.stream()
                .map(EventParticipantEntity::getUserId)
                .distinct()
                .toList();

        List<UserDto> users = userRestClient.getUsersByIds(applicantIds);

        Map<Long, UserDto> usersMap = users.stream()
                .collect(Collectors.toMap(
                        UserDto::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return pendingRelations.stream().collect(Collectors.groupingBy(
                EventParticipantEntity::getEventId,
                Collectors.mapping(relation -> {
                    UserDto userDto = usersMap.get(relation.getUserId());
                    if (userDto == null) {
                        throw new UserRestClientException("Не удалось получить данные пользователя из микросервиса user");
                    }
                    String locationStr = userDto.getCity() + ", " + userDto.getDistrictName();
                    return new IncomingRequestDto(
                            relation.getId(),
                            userDto.getUserId(),
                            userDto.getFirstName(),
                            userDto.getLastName(),
                            locationStr,
                            userDto.getReputation(),
                            relation.getStatus().name()
                    );
                }, Collectors.toList())
        ));
    }


    @Override
    @Transactional(readOnly = true)
    public List<OutgoingRequestDto> getOutgoingRequestsForGuest(Long guestId) {
        log.info("[ParticipantService - INFO] Сбор исходящих заявок для гостя: {}", guestId);

            List<ParticipantStatus> targetStatuses = List.of(
                    ParticipantStatus.PENDING,
                    ParticipantStatus.RE_PENDING,
                    ParticipantStatus.REJECTED,
                    ParticipantStatus.BANNED
            );

        List<EventParticipantEntity> myRelations = participantRepository.findActiveOutgoingRequests(guestId, targetStatuses);

        if (myRelations.isEmpty()) {
            return Collections.emptyList();
        }

        return myRelations.stream()
                .map(relation -> {
                    EventEntity event = eventRepository.findById(relation.getEventId())
                            .orElseThrow(() -> new EventNotFoundException(relation.getEventId()));

                    return new OutgoingRequestDto(
                            event.getId(),
                            event.getTitle(),
                            event.getEventDate(),
                            event.getLocationName(),
                            relation.getStatus().name(), // Передаст строку "PENDING" или "REJECTED"
                            event.getOrganizerId()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantUserDto> getJoinedParticipants(Long eventId) {
        log.info("[ParticipantService] Запрос списка одобренных участников для события {}", eventId);

        // 1. Находим событие, чтобы определить, кто является организатором
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Long organizerId = event.getOrganizerId();

        // 2. Получаем список одобренных участников со статусом JOINED
        List<EventParticipantEntity> participants =
                participantRepository.findAllByEventIdAndStatus(eventId, ParticipantStatus.JOINED);
        List<ParticipantUserDto> dtoList = new ArrayList<>();

        // 3. Используем LinkedHashSet: он автоматически исключит дубликаты и сохранит порядок (организатор будет первым)
        Set<Long> uniqueUserIds = new LinkedHashSet<>();
        if (organizerId != null) {
            uniqueUserIds.add(organizerId);
        }
        for (EventParticipantEntity p : participants) {
            if (p.getUserId() != null) {
                uniqueUserIds.add(p.getUserId());
            }
        }

        List<UserDto> users = userRestClient.getUsersByIds(new ArrayList<>(uniqueUserIds));

        Map<Long, UserDto> usersMap = users.stream()
                .collect(Collectors.toMap(
                        UserDto::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing // при дублировании оставляем первый вариант
                ));

        for (Long userId : uniqueUserIds) {
            String fName = "Сосед";
            String lName = "#" + userId;
            UserDto userDto = usersMap.get(userId);
            if (userDto != null) {
                fName = userDto.getFirstName() != null ? userDto.getFirstName() : "Сосед";
                lName = userDto.getLastName() != null ? userDto.getLastName() : "";
            }
            ParticipantUserDto dto = new ParticipantUserDto();
            dto.setUserId(userId);
            dto.setFirstName(fName);
            dto.setLastName(lName);
            dto.setAvatarUrl(userDto != null ? userDto.getAvatarUrl() : null);
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional
    public void kickParticipantByUserId(Long eventId, Long kickedUserId, Long organizerId) {
        log.info("[ParticipantService] Организатор {} инициировал кик юзера {} через поиск по userId", organizerId, kickedUserId);
        EventParticipantEntity participant = participantRepository.findByEventIdAndUserId(eventId, kickedUserId)
                .orElseThrow(() -> new IllegalStateException("Участник не найден в данном событии"));

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new IllegalStateException("Исключить из встречи можно только активного участника");
        }

        this.rejectParticipant(eventId, participant.getId(), organizerId);
    }

    @Override
    @Transactional
    public void kickParticipantInternal(Long eventId, Long kickedUserId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        kickParticipantByUserId(eventId, kickedUserId, event.getOrganizerId());
    }

    @Override
    @Transactional
    public void restoreKickedParticipant(Long eventId, Long userId, Long organizerId) {
        log.info("[ParticipantService] Организатор {} возвращает юзера {} в событие {}", organizerId, userId, eventId);

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        assertOrganizer(event, organizerId);
        if (event.getOrganizerId().equals(userId)) {
            throw new IllegalStateException("Организатор уже является участником встречи");
        }

        EventParticipantEntity participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("Запись участника не найдена"));

        if (participant.getStatus() == ParticipantStatus.BANNED) {
            throw new IllegalStateException("Кандидат окончательно отклонён — вернуть нельзя");
        }
        if (participant.getStatus() != ParticipantStatus.KICKED) {
            throw new IllegalStateException("Вернуть можно только исключённого участника (статус KICKED)");
        }

        if (eventRepository.tryIncrementParticipants(eventId) == 0) {
            throw new ParticipantLimitExceededException();
        }

        participant.setStatus(ParticipantStatus.JOINED);
        participantRepository.save(participant);
        appMetrics.vstrechaVstuplenie();
        log.info("[Встречи] Пользователь {} возвращён во встречу {}", userId, eventId);
        syncCurrentParticipants(eventId);

        UserDto userDto = userRestClient.getUserById(userId)
                .orElseThrow(() -> new UserRestClientException("Не удалось получить данные пользователя"));

        enqueueUserJoined(eventId, userId, userDto);
        log.info("[ParticipantService - Outbox] Возврат: юзер {} снова будет в чате встречи {}", userId, eventId);

        try {
            notificationProducer.sendEvent(
                    userId,
                    organizerId,
                    "Организатор",
                    "",
                    NotificationType.EVENT_JOIN_SUCCESS,
                    event.getId(),
                    null,
                    "Вас снова добавили в групповой чат встречи «%s»".formatted(event.getTitle()),
                    "JOINER_CHAT"
            );
        } catch (Exception e) {
            log.error("[Notification - ERROR] Не удалось уведомить о возврате: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantUserDto> getBannedParticipants(Long eventId, Long organizerId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        assertOrganizer(event, organizerId);

        // Список для «Вернуть» — только кикнутые (KICKED), не вечный BANNED
        List<EventParticipantEntity> kicked =
                participantRepository.findAllByEventIdAndStatus(eventId, ParticipantStatus.KICKED);
        if (kicked.isEmpty()) {
            return List.of();
        }

        List<Long> ids = kicked.stream().map(EventParticipantEntity::getUserId).filter(Objects::nonNull).toList();
        List<UserDto> users = userRestClient.getUsersByIds(ids);
        Map<Long, UserDto> usersMap = users.stream()
                .collect(Collectors.toMap(UserDto::getUserId, Function.identity(), (a, b) -> a));

        List<ParticipantUserDto> result = new ArrayList<>();
        for (Long uid : ids) {
            UserDto u = usersMap.get(uid);
            ParticipantUserDto dto = new ParticipantUserDto();
            dto.setUserId(uid);
            dto.setFirstName(u != null && u.getFirstName() != null ? u.getFirstName() : "Сосед");
            dto.setLastName(u != null && u.getLastName() != null ? u.getLastName() : "#" + uid);
            dto.setAvatarUrl(u != null ? u.getAvatarUrl() : null);
            result.add(dto);
        }
        return result;
    }


    private void assertOrganizer(EventEntity event, Long organizerId) {
        if (organizerId == null || !event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Только организатор может выполнить это действие");
        }
    }

    /** Организатор всегда занимает 1 слот; JOINED — остальные. Чинит рассинхрон счётчика. */
    private int syncCurrentParticipants(Long eventId) {
        long joined = participantRepository.countByEventIdAndStatus(eventId, ParticipantStatus.JOINED);
        int correct = 1 + (int) joined;
        eventRepository.setCurrentParticipants(eventId, correct);
        log.info("[ParticipantService] Счётчик участников eventId={} = {}", eventId, correct);
        return correct;
    }

    private EventDto toDtoWithSyncedCount(Long eventId) {
        syncCurrentParticipants(eventId);
        EventEntity entity = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        EventDto dto = eventMapper.toDto(entity);
        dto.setCanVoteReputation(eventLifecycleService.canVoteReputation(entity));
        dto.setReputationOpensAt(eventLifecycleService.reputationOpensAt(entity));
        dto.setReputationClosesAt(eventLifecycleService.reputationClosesAt(entity));
        return dto;
    }

    private void assertEventStillOpenForJoin(EventEntity event) {
        if (event.getEventDate() != null && !event.getEventDate().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Встреча уже началась, новые заявки не принимаются");
        }
    }

    private void sendUserLeftKafka(Long eventId, Long userId, String status, boolean isKicked) {
        String firstName = "Участник";
        String lastName = "";
        try {
            UserDto leftUser = userRestClient.getUserById(userId).orElse(null);
            if (leftUser != null) {
                firstName = leftUser.getFirstName() != null ? leftUser.getFirstName() : "Участник";
                lastName = leftUser.getLastName() != null ? leftUser.getLastName() : "";
            }
        } catch (Exception nameEx) {
            log.warn("[Outbox] Не удалось получить имя уходящего userId={}: {}", userId, nameEx.getMessage());
        }
        outboxService.enqueue(
                KafkaTopics.USER_LEFT,
                eventId,
                EventUserLeftEvent.builder()
                        .eventId(eventId)
                        .userId(userId)
                        .status(status)
                        .kicked(isKicked)
                        .firstName(firstName)
                        .lastName(lastName)
                        .build()
        );
    }

    private void enqueueUserJoined(Long eventId, Long userId, UserDto userDto) {
        outboxService.enqueue(
                KafkaTopics.USER_JOINED,
                eventId,
                new EventUserJoinedEvent(eventId, userId, userDto.getFirstName(), userDto.getLastName())
        );
    }

    private void triggerKafkaLeftTopic(Long eventId, Long userId, String status) {
        sendUserLeftKafka(eventId, userId, status, false);
    }

}