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
public class CoverModeConstraintFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_cover_mode_check");
            jdbcTemplate.execute("""
                    ALTER TABLE users ADD CONSTRAINT users_cover_mode_check
                    CHECK (cover_mode IS NULL OR cover_mode IN ('COLOR', 'TRANSPARENT', 'PHOTO'))
                    """);
            log.info("[User] Ограничение users_cover_mode_check обновлено: COLOR, TRANSPARENT, PHOTO");
        } catch (Exception e) {
            log.warn("[User] Не удалось обновить users_cover_mode_check: {}", e.getMessage());
        }
    }
}
