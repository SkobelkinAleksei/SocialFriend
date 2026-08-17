package org.example.chat.entity;

public final class ChatSystemMessages {

    public static final String EVENT_CANCELED = "Встреча отменена, чат будет удален";
    public static final String CHAT_CLOSING_FEW_PEOPLE = "В чате меньше двух человек — чат закрывается";
    public static final String KEEP_CHAT_SAVED =
            "Большинство проголосовало за сохранение чата — он остаётся. Если чат вам больше не нужен, вы можете из него выйти.";
    public static final String KEEP_CHAT_QUESTION =
            "Чат будет удалён, если «за» наберётся меньше 65%. Молчание не считается «за».";
    public static final String KEEP_CHAT_YES = "Оставить чат";
    public static final String KEEP_CHAT_NO = "Удалить чат";
    public static final String ROOM_DELETED = "[ROOM_DELETED]";

    private ChatSystemMessages() {
    }
}
