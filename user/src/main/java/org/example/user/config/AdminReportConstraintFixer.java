package org.example.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminReportConstraintFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE admin_reports ADD COLUMN IF NOT EXISTS details VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE admin_reports DROP CONSTRAINT IF EXISTS admin_reports_category_check");
            jdbcTemplate.execute("""
                    ALTER TABLE admin_reports ADD CONSTRAINT admin_reports_category_check
                    CHECK (category IN ('POST', 'COMMENT', 'PHOTO', 'EVENT', 'MESSAGE', 'CHAT'))
                    """);
            log.info("[User] Ограничение admin_reports_category_check обновлено");
        } catch (Exception e) {
            log.warn("[User] Не удалось обновить admin_reports_category_check: {}", e.getMessage());
        }
    }
}
