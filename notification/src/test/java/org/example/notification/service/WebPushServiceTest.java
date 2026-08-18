package org.example.notification.service;

import org.example.notification.entity.PushSubscriptionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WebPush — не слать баннер, пока устройство смотрит сайт")
class WebPushServiceTest {

    @Test
    @DisplayName("Нет отметки «смотрю» — push можно слать")
    void notViewingAllowsPush() {
        PushSubscriptionEntity row = PushSubscriptionEntity.builder()
                .userId(1L)
                .endpoint("https://push.example/1")
                .p256dh("x")
                .auth("y")
                .build();
        assertFalse(WebPushService.isViewingNow(row, Instant.now()));
    }

    @Test
    @DisplayName("Отметка ещё действует — push не слать")
    void viewingSkipsPush() {
        Instant now = Instant.parse("2026-08-18T12:00:00Z");
        PushSubscriptionEntity row = PushSubscriptionEntity.builder()
                .userId(1L)
                .endpoint("https://push.example/1")
                .p256dh("x")
                .auth("y")
                .foregroundUntil(now.plusSeconds(60))
                .build();
        assertTrue(WebPushService.isViewingNow(row, now));
    }

    @Test
    @DisplayName("Отметка просрочена — снова слать")
    void expiredAllowsPush() {
        Instant now = Instant.parse("2026-08-18T12:00:00Z");
        PushSubscriptionEntity row = PushSubscriptionEntity.builder()
                .userId(1L)
                .endpoint("https://push.example/1")
                .p256dh("x")
                .auth("y")
                .foregroundUntil(now.minusSeconds(1))
                .build();
        assertFalse(WebPushService.isViewingNow(row, now));
    }
}
