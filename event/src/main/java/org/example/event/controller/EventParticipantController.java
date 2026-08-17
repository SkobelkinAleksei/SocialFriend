package org.example.event.controller;

import com.example.common.dto.event.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.event.dto.ParticipantUserDto;
import org.example.event.entity.EventParticipantEntity;
import org.example.event.service.EventParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/events/{eventId}/participants")
@RestController
public class EventParticipantController {

    private final EventParticipantService participantService;

    // =========================================================================
    // ВЗАИМОДЕЙСТВИЕ С КАРТОЧКОЙ СОБЫТИЯ (ДЕЙСТВИЯ ОБЫЧНОГО ПОЛЬЗОВАТЕЛЯ/ГОСТЯ)

    /** Сразу вступить в открытое событие (кнопка "Вступить в событие") */
    @PostMapping("/join")
    public ResponseEntity<EventDto> joinEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[ParticipantController - INFO] Юзер {} вступает в открытое событие {}", currentUserId, eventId);
        return ResponseEntity.ok().body(participantService.joinEvent(eventId, currentUserId));
    }

    /** Подать заявку на приватное событие (кнопка "Оставить заявку") */
    @PostMapping("/apply")
    public ResponseEntity<EventDto> applyToEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[ParticipantController - INFO] Юзер {} подает заявку на приватное событие {}", currentUserId, eventId);
        return ResponseEntity.ok().body(participantService.applyToEvent(eventId, currentUserId));
    }

    /** Выйти из события или отозвать ранее поданную заявку */
    @PostMapping("/leave")
    public ResponseEntity<EventDto> leaveEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[ParticipantController - INFO] Юзер {} покидает событие {}", currentUserId, eventId);
        return ResponseEntity.ok().body(participantService.leaveEvent(eventId, currentUserId));
    }

    // =========================================================================
    // МОДЕРАЦИЯ И УПРАВЛЕНИЕ УЧАСТНИКАМИ (ДЕЙСТВИЯ СОЗДАТЕЛЯ/ОРГАНИЗАТОРА)

    /** Одобрить входящую заявку соседа (доступно только организатору встречи) */
    @PatchMapping("/{participantId}/approve")
    public ResponseEntity<Void> approveParticipant(
            @PathVariable Long eventId,
            @PathVariable Long participantId,
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        log.info("[ParticipantController - INFO] Организатор {} одобряет юзера {} в событие {}", organizerId, participantId, eventId);
        participantService.approveParticipant(eventId, participantId, organizerId);
        return ResponseEntity.ok().build();
    }

    /** Отклонить / отменить входящую заявку соседа (доступно только организатору) */
    @PatchMapping("/{participantId}/reject")
    public ResponseEntity<Void> rejectParticipant(
            @PathVariable Long eventId,
            @PathVariable Long participantId,
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        log.info("[ParticipantController - INFO] Организатор {} отклоняет юзера {} в событии {}", organizerId, participantId, eventId);
        participantService.rejectParticipant(eventId, participantId, organizerId);
        return ResponseEntity.ok().build();
    }

    /** Исключить (кикнуть) одобренного участника из текущего события */
    @DeleteMapping("/kick/{kickedUserId}")
    public ResponseEntity<Void> kickParticipantByUserId(
            @PathVariable Long eventId,
            @PathVariable Long kickedUserId,
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        log.info("[ParticipantController] Запрос от организатора {} на исключение юзера {} из события {}", organizerId, kickedUserId, eventId);
        participantService.kickParticipantByUserId(eventId, kickedUserId, organizerId);
        return ResponseEntity.ok().build();
    }

    /** Вернуть ранее исключённого участника во встречу и групповой чат */
    @PostMapping("/restore/{userId}")
    public ResponseEntity<Void> restoreKickedParticipant(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        log.info("[ParticipantController] Организатор {} возвращает юзера {} в событие {}", organizerId, userId, eventId);
        participantService.restoreKickedParticipant(eventId, userId, organizerId);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ ИНФОРМАЦИИ, СПИСКОВ И СТАТУСОВ УЧАСТИЯ

    /** Запросить детальный статус текущего пользователя по конкретной встрече */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getUserStatus(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[ParticipantController - INFO] Запрос статуса для юзера {} по встрече {}", currentUserId, eventId);
        String status = participantService.getUserStatusForEvent(eventId, currentUserId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    /** Запросить карту глобальных статусов участия текущего пользователя (ID встречи -> Статус) */
    @GetMapping("/statuses")
    public ResponseEntity<Map<Long, String>> getUserStatuses(
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        log.info("[ParticipantController - INFO] Запрос карты статусов для юзера {}", currentUserId);
        return ResponseEntity.ok(participantService.getUserParticipantsMap(currentUserId));
    }

    /** Получить список всех одобренных участников для отображения в интерфейсе карточки */
    @GetMapping
    public ResponseEntity<List<ParticipantUserDto>> getParticipants(
            @PathVariable Long eventId
    ) {
        log.info("[ParticipantController - INFO] Запрос списка участников для события {}", eventId);
        return ResponseEntity.ok(participantService.getJoinedParticipants(eventId));
    }

    /** Список исключённых (для организатора — кнопка «Вернуть») */
    @GetMapping("/banned")
    public ResponseEntity<List<ParticipantUserDto>> getBannedParticipants(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long organizerId
    ) {
        return ResponseEntity.ok(participantService.getBannedParticipants(eventId, organizerId));
    }
}