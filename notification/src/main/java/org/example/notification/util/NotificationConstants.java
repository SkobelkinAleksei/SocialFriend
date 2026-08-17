package org.example.notification.util;

public final class NotificationConstants {
    private NotificationConstants() {
    }

    /** Системный маркер удаления лайка (дизлайк) */
    public static final String SYSTEM_LIKE_REMOVED = "__SYSTEM_LIKE_REMOVED__";

    /** Синхронизация счётчика лайков без тоста (настройки тостов выключены / анти-спам) */
    public static final String SYSTEM_LIKE_SYNC = "__LIKE_SYNC__";

    /** Базовый префикс WebSocket топика для отправки уведомлений */
    public static final String WS_TOPIC_PREFIX = "/topic/notifications-";
}