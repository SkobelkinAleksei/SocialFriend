package org.example.user.config;

import com.example.common.kafka.UserReputationChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.service.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserReputationKafkaListener {

    private final UserService userService;

    @KafkaListener(
            topics = "user-reputation-topic",
            groupId = "user-group-reputation",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReputationUpdate(UserReputationChangedEvent event) {
        log.info("[Kafka Consumer - User] Обновление репутации: userId={}, change={}",
                event.getUserId(), event.getChange());
        userService.updateReputation(event.getUserId(), event.getChange());
    }
}
