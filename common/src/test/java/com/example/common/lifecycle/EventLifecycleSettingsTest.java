package com.example.common.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventLifecycleSettings — окна жизни встречи")
public class EventLifecycleSettingsTest {

    private final EventLifecycleSettings settings = new EventLifecycleSettings();

    @Nested
    @DisplayName("Окно репутации")
    class ReputationWindow {

        @Test
        @DisplayName("null даты — окно закрыто")
        void nullDates() {
            assertFalse(settings.isReputationOpen(null, LocalDateTime.now()));
            assertFalse(settings.isReputationOpen(LocalDateTime.now(), null));
        }

        @Test
        @DisplayName("Сразу после начала встречи оценки ещё закрыты (нужно 180 минут)")
        void justAfterStartClosed() {
            LocalDateTime start = LocalDateTime.of(2026, 8, 16, 12, 0);
            assertFalse(settings.isReputationOpen(start, start.plusMinutes(179)));
        }

        @Test
        @DisplayName("Через 180 минут окно открыто, через 7 суток — уже нет")
        void openThenClosed() {
            LocalDateTime start = LocalDateTime.of(2026, 8, 16, 12, 0);
            assertTrue(settings.isReputationOpen(start, start.plusMinutes(180)));
            assertFalse(settings.isReputationOpen(start, start.plusMinutes(7 * 24 * 60)));
        }
    }

    @Nested
    @DisplayName("Опрос «оставить чат»")
    class KeepChat {

        @Test
        @DisplayName("Меньше 2 голосующих — чат не оставляем даже при 100% «да»")
        void tooFewPeople() {
            assertFalse(settings.keepChat(1, 1));
        }

        @Test
        @DisplayName("65% «да» при 4 участниках: 3/4 = 75% — оставляем, 2/4 = 50% — нет")
        void threshold() {
            assertTrue(settings.keepChat(3, 4));
            assertFalse(settings.keepChat(2, 4));
        }
    }

    @Test
    @DisplayName("Подписи минут/часов/суток на русском")
    void labels() {
        assertEquals("1 минуту", settings.minutesLabel(1));
        assertEquals("2 минуты", settings.minutesLabel(2));
        assertEquals("5 минут", settings.minutesLabel(5));
        assertEquals("1 час", settings.minutesLabel(60));
        assertEquals("2 часа", settings.minutesLabel(120));
        assertEquals("1 сутки", settings.minutesLabel(24 * 60));
        assertEquals("2 суток", settings.minutesLabel(48 * 60));
    }

    @Test
    @DisplayName("Напоминание за час до начала")
    void remindAt() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 16, 18, 0);
        assertEquals(LocalDateTime.of(2026, 8, 16, 17, 0), settings.remindAt(start));
    }
}
