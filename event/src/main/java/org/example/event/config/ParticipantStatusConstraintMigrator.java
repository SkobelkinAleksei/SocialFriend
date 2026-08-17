package org.example.event.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update не обновляет CHECK-ограничения enum.
 * Без KICKED в constraint кик откатывается в БД, а побочные эффекты могут уже уйти.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantStatusConstraintMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE event_participants DROP CONSTRAINT IF EXISTS event_participants_status_check");
            jdbcTemplate.execute("""
                    ALTER TABLE event_participants
                    ADD CONSTRAINT event_participants_status_check
                    CHECK (status IN ('PENDING', 'JOINED', 'REJECTED', 'RE_PENDING', 'BANNED', 'KICKED'))
                    """);
            log.info("[Schema] event_participants_status_check обновлён (добавлен KICKED)");
        } catch (Exception e) {
            log.error("[Schema] Не удалось обновить CHECK для status: {}", e.getMessage());
        }
    }
}
