package org.example.friend.config;

import com.example.common.kafka.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.entity.UserReferenceEntity;
import org.example.friend.repository.UserReferenceRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendKafkaListener {

    private final UserReferenceRepository userReferenceRepository;

    @KafkaListener(topics = "user-registered", groupId = "friend-group")
    public void listenUserRegistration(UserRegisteredEvent event) {
        log.info("[FriendKafkaListener - INFO] Получено событие регистрации пользователя: {}", event.getId());

        UserReferenceEntity userRef = new UserReferenceEntity();
        userRef.setId(event.getId());

        userReferenceRepository.save(userRef);
        log.info("[FriendKafkaListener - INFO] ID пользователя {} успешно синхронизирован", event.getId());
    }
}