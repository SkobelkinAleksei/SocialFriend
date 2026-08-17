package org.example.scheduler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.entity.JobStatus;
import org.example.scheduler.entity.JobType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ScheduledJobConstraintMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        replaceCheck("scheduled_jobs", "type", "scheduled_jobs_type_check", JobType.values());
        replaceCheck("scheduled_jobs", "status", "scheduled_jobs_status_check", JobStatus.values());
    }

    private void replaceCheck(String table, String column, String constraint, Enum<?>[] values) {
        String allowed = Arrays.stream(values)
                .map(type -> "'" + type.name() + "'")
                .collect(Collectors.joining(", "));
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                            + " CHECK (" + column + " IN (" + allowed + "))"
            );
            log.info("[Schema] {} обновлён", constraint);
        } catch (Exception e) {
            log.warn("[Schema] Не удалось обновить {}: {}", constraint, e.getMessage());
        }
    }
}
