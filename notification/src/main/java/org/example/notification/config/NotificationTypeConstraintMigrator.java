package org.example.notification.config;

import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hibernate ddl-auto=update не переписывает CHECK по enum.
 * Новые типы планировщика иначе падают с notifications_type_check.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class NotificationTypeConstraintMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String allowed = Arrays.stream(NotificationType.values())
                .map(type -> "'" + type.name() + "'")
                .collect(Collectors.joining(", "));
        try {
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
            jdbcTemplate.execute(
                    "ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (" + allowed + "))"
            );
            log.info("[Schema] notifications_type_check обновлён под актуальные типы уведомлений");
        } catch (Exception e) {
            log.error("[Schema] Не удалось обновить CHECK для notifications.type: {}", e.getMessage());
        }
    }
}
