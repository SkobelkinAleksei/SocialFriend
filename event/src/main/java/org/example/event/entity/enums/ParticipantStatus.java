package org.example.event.entity.enums;

public enum ParticipantStatus {
    PENDING,    // Заявка на рассмотрении (первая)
    JOINED,     // Участник принят / вступил
    REJECTED,   // Первый отказ — можно подать повторно
    RE_PENDING, // Повторная заявка на рассмотрении
    BANNED,     // Второй отказ — навсегда, без restore
    KICKED      // Исключён из JOINED — только организатор может вернуть
}
