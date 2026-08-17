package org.example.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ChatRoomTypeMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("UPDATE chat_rooms SET room_type = 'EVENT' WHERE room_type IS NULL");
            jdbcTemplate.execute("UPDATE chat_rooms SET add_members_policy = 'OWNER_ONLY' WHERE add_members_policy IS NULL");
            jdbcTemplate.execute("UPDATE chat_rooms SET rename_policy = 'OWNER_ONLY' WHERE rename_policy IS NULL");
            jdbcTemplate.execute("UPDATE chat_rooms SET avatar_policy = 'OWNER_ONLY' WHERE avatar_policy IS NULL");
            log.info("[Schema] chat_rooms: заполнены room_type и политики");
        } catch (Exception e) {
            log.warn("[Schema] Не удалось заполнить поля chat_rooms: {}", e.getMessage());
        }
    }
}
