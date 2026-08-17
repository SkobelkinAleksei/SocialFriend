package org.example.event.service;

import com.example.common.dto.event.EventDto;
import com.example.common.dto.event.IncomingRequestDto;
import com.example.common.dto.event.OutgoingRequestDto;
import org.example.event.dto.ParticipantUserDto;

import java.util.List;
import java.util.Map;

public interface EventParticipantService {

    // ВЗАИМОДЕЙСТВИЕ С КАРТОЧКОЙ СОБЫТИЯ (ДЕЙСТВИЯ ОБЫЧНОГО ПОЛЬЗОВАТЕЛЯ/ГОСТЯ)

    /** Сразу вступить в открытое событие без подтверждения владельца */
    EventDto joinEvent(Long eventId, Long userId);

    /** Подать заявку на участие в приватном событии */
    EventDto applyToEvent(Long eventId, Long userId);

    /** Добровольно покинуть событие или отозвать ранее поданную заявку */
    EventDto leaveEvent(Long eventId, Long userId);

    // =========================================================================
    // МОДЕРАЦИЯ И УПРАВЛЕНИЕ УЧАСТНИКАМИ (ДЕЙСТВИЯ СОЗДАТЕЛЯ/ОРГАНИЗАТОРА)

    /** Одобрить входящую заявку пользователя на участие в событии */
    void approveParticipant(Long eventId, Long participantId, Long organizerId);

    /** Отклонить входящую заявку пользователя на участие в событии */
    void rejectParticipant(Long eventId, Long participantId, Long organizerId);

    /** Исключить (кикнуть) одобренного участника из события */
    void kickParticipantByUserId(Long eventId, Long kickedUserId, Long organizerId);

    void kickParticipantInternal(Long eventId, Long kickedUserId);

    /** Вернуть ранее исключённого (BANNED) участника обратно во встречу и чат */
    void restoreKickedParticipant(Long eventId, Long userId, Long organizerId);

    // =========================================================================
    // ПОЛУЧЕНИЕ ИНФОРМАЦИИ, СПИСКОВ И СТАТУСОВ УЧАСТИЯ

    /** Получить текущий текстовый статус конкретного пользователя для события */
    String getUserStatusForEvent(Long eventId, Long userId);

    /** Получить карту связей (ID события -> Статус участия) для конкретного пользователя */
    Map<Long, String> getUserParticipantsMap(Long userId);

    /** Получить список всех одобренных участников встречи для отображения в карточке */
    List<ParticipantUserDto> getJoinedParticipants(Long eventId);

    /** Список исключённых (BANNED) — только для организатора, чтобы можно было вернуть */
    List<ParticipantUserDto> getBannedParticipants(Long eventId, Long organizerId);

    /** Собрать карту всех активных входящих заявок (EventId -> Список заявок) для организатора */
    Map<Long, List<IncomingRequestDto>> getIncomingRequestsForOrganizer(Long organizerId);

    /** Получить список всех исходящих заявок пользователя (где он ожидает одобрения) */
    List<OutgoingRequestDto> getOutgoingRequestsForGuest(Long guestId);
}