package org.example.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatPollPurpose;
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
public class ChatPollPurposeConstraintMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String allowed = Arrays.stream(ChatPollPurpose.values())
                .map(type -> "'" + type.name() + "'")
                .collect(Collectors.joining(", "));
        try {
            jdbcTemplate.execute("ALTER TABLE chat_polls DROP CONSTRAINT IF EXISTS chat_polls_purpose_check");
            jdbcTemplate.execute(
                    "ALTER TABLE chat_polls ADD CONSTRAINT chat_polls_purpose_check CHECK (purpose IS NULL OR purpose IN (" + allowed + "))"
            );
            log.info("[Schema] chat_polls_purpose_check обновлён");
        } catch (Exception e) {
            log.warn("[Schema] CHECK для chat_polls.purpose не обновлён: {}", e.getMessage());
        }
    }
}
