package org.example.event.controller;

import com.example.common.dto.event.EventDto;
import com.example.common.dto.event.IncomingRequestDto;
import com.example.common.dto.event.OutgoingRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.dto.CreateEventDto;
import org.example.event.dto.MyEventTabCountersDto;
import org.example.event.dto.NeighborEventCountersDto;
import org.example.event.entity.enums.EventCategory;
import org.example.event.entity.enums.EventUserFilter;
import org.example.event.entity.enums.NeighborEventFilter;
import org.example.event.service.EventParticipantService;
import org.example.event.service.EventService;
import org.example.event.validation.OnCreate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/events")
@RestController
public class EventController {

    private final EventService eventService;
    private final EventParticipantService eventParticipantService;

    // =========================================================================
    // БАЗОВЫЙ ЖИЗНЕННЫЙ ЦИКЛ СОБЫТИЯ

    /** Поставить новую точку на карту (создать встречу) */
    @PostMapping
    public ResponseEntity<EventDto> createEvent(
            @Validated(OnCreate.class) @RequestBody CreateEventDto createEventDto,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("[EventController - INFO] Запрос на создание события от пользователя: {}", userId);
        return ResponseEntity.ok(eventService.createEvent(createEventDto, userId));
    }

    /** Посмотреть детальную информацию о конкретном событии */
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[EventController - INFO] Запрос на получение события с id: {} от пользователя: {}", id, currentUserId);
        return ResponseEntity.ok(eventService.getEventById(id, currentUserId));
    }

    /** Отредактировать данные существующего события его создателем */
    @PutMapping("/{id}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateEventDto createEventDto,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[EventController - INFO] Запрос на обновление события с id: {} от пользователя: {}", id, currentUserId);
        return ResponseEntity.ok(eventService.updateEvent(id, createEventDto, currentUserId));
    }

    /** Снять точку с карты (полностью удалить встречу организатором) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[EventController - INFO] Запрос на удаление события с id: {} от пользователя: {}", id, currentUserId);
        eventService.deleteEvent(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // ГЕО-КАРТА И ГЛОБАЛЬНЫЙ ТРАФИК ОБЪЕКТОВ

    /** Загрузить маркеры встреч в видимой зоне карты с фильтром по выбранной категории */
    @GetMapping
    public ResponseEntity<List<EventDto>> getEvents(
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) BigDecimal minLat,
            @RequestParam(required = false) BigDecimal maxLat,
            @RequestParam(required = false) BigDecimal minLng,
            @RequestParam(required = false) BigDecimal maxLng,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("[EventController - INFO] Запрос списка событий от пользователя: {}. Фильтр: {}", userId, category);
        return ResponseEntity.ok(eventService.getEvents(category, minLat, maxLat, minLng, maxLng, userId));
    }

    // =========================================================================
    // УПРАВЛЕНИЕ ЗАЯВКАМИ НА УЧАСТИЕ ДЛЯ ПРОФИЛЕЙ

    /** Собрать карту всех активных входящих заявок (EventId -> Список заявок) для организатора */
    @GetMapping("/incoming")
    public ResponseEntity<Map<Long, List<IncomingRequestDto>>> getIncomingRequests(
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        log.info("[EventController - INFO] Организатор {} запрашивает входящие заявки", organizerId);
        return ResponseEntity.ok(eventParticipantService.getIncomingRequestsForOrganizer(organizerId));
    }

    /** Получить список всех исходящих заявок пользователя (где он гость и ожидает одобрения) */
    @GetMapping("/outgoing")
    public ResponseEntity<List<OutgoingRequestDto>> getOutgoingRequests(
            @RequestHeader("X-User-Id") Long guestId
    ) {
        log.info("[EventController - INFO] Пользователь {} запрашивает исходящие заявки", guestId);
        return ResponseEntity.ok(eventParticipantService.getOutgoingRequestsForGuest(guestId));
    }

    // =========================================================================
    // СТАТИСТИКА, СПИСКИ И ЛИЧНЫЕ КАБИНЕТЫ ПОЛЬЗОВАТЕЛЕЙ

    /** Получить списки будущих или прошедших событий текущего авторизованного пользователя */
    @GetMapping("/list")
    public ResponseEntity<List<EventDto>> getEventsByUser(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "filter") EventUserFilter filter
    ) {
        log.info("[EventController] Запрос активности пользователя {} по фильтру: {}", userId, filter);
        return ResponseEntity.ok(eventService.getEventsByEnumFilter(userId, filter));
    }

    /** Счётчики вкладок текущего пользователя (без выгрузки полных списков) */
    @GetMapping("/me/tab-counters")
    public ResponseEntity<MyEventTabCountersDto> getMyTabCounters(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(eventService.getMyTabCounters(userId));
    }

    /** Получить публичный список прошедших или будущих событий другого пользователя */
    @GetMapping("/public/user/{neighborId}")
    public ResponseEntity<List<EventDto>> getPublicEventsByNeighbor(
            @PathVariable("neighborId") Long neighborId,
            @RequestParam(value = "filter") NeighborEventFilter filter,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[EventController] Гость {} запрашивает события соседа {} по фильтру: {}", currentUserId, neighborId, filter);
        return ResponseEntity.ok(eventService.getPublicEventsByNeighborId(neighborId, filter, currentUserId));
    }

    /** Получить числовые счетчики созданных и посещенных событий конкретного соседа для аналитики профиля */
    @GetMapping("/public/user/{neighborId}/counters")
    public ResponseEntity<NeighborEventCountersDto> getNeighborEventCounters(
            @PathVariable("neighborId") Long neighborId
    ) {
        log.info("[EventController] Запрос счетчиков событий для соседа: {}", neighborId);
        return ResponseEntity.ok(eventService.getEventCountersByUserId(neighborId));
    }
}