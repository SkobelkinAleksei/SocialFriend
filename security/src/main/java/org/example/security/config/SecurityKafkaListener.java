package org.example.security.config;

import lombok.RequiredArgsConstructor;
import org.example.eventskafka.UserRegisteredEvent;
import org.example.security.service.SecurityUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityKafkaListener {

    private final SecurityUserService userSecurityService;

    @KafkaListener(
            topics = "user-registered",
            groupId = "security-group"
    )
    public void listenUserRegistered(UserRegisteredEvent event) {
        userSecurityService.createUserFromEvent(event);
    }
}