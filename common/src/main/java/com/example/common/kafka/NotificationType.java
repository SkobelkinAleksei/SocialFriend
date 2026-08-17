package com.example.common.kafka;

public enum NotificationType {
    FRIEND_REQUEST_SENT,
    FRIEND_REQUEST_ACCEPTED,
    FRIEND_REQUEST_REJECTED,
    FRIEND_REQUEST_CANCELLED,
    POST_LIKE,
    PHOTO_LIKE,           // Лайк фото (галерея, пост, аватар)
    NEW_COMMENT,          // Комментарии к постам
    COMMENT_REPLY,        // Ответ / тег в комментариях
    COMMENT_VOTE,         // Лайк/дизлайк комментария (только live-счётчик, без тоста)
    COMMENT_EDIT,         // Правка текста комментария (только live, без тоста)
    NEW_CHAT_MESSAGE,     // Личные сообщения
    EVENT_JOIN_REQUEST,    // Заявки на события (когда просятся)
    EVENT_JOIN_SUCCESS,    // Когда успешно вступил / одобрили заявку
    EVENT_JOIN_REJECTED, // Первый отказ (можно подать повторно)
    EVENT_JOIN_BANNED,   // Окончательный бан (повторно нельзя)
    REPUTATION_UPDATE,     // Изменения репутации
    EVENT_KICK,          // Принудительное исключение участника организатором
    EVENT_REMIND_1H,       // Встреча начнётся через час
    EVENT_REPUTATION_OPEN, // Можно ставить оценки участникам
    EVENT_CANCELLED,       // Встреча отменена
    EVENT_CHAT_KEEP_VOTE,  // Голосование за сохранение чата встречи
    ADMIN_CONTENT_REMOVED  // Модерация: контент удалён, без перехода
}