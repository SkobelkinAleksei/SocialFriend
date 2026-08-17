package org.example.friend.service.admin;

import com.example.common.kafka.UserAccountStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.repository.FriendRequestRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountStatusListener {

    private final FriendRequestRepository friendRequestRepository;

    @KafkaListener(
            topics = "user-account-status-changed",
            groupId = "friend-admin-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onAccountStatus(UserAccountStatusChangedEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        String status = event.getStatus() == null ? "" : event.getStatus().trim().toUpperCase();
        if (!"BANNED".equals(status) && !"DELETED".equals(status)) {
            return;
        }
        int removed = friendRequestRepository.deletePendingInvolving(event.getUserId(), FriendRequestStatus.PENDING);
        log.info("[Admin] Снято {} заявок в друзья для userId={}", removed, event.getUserId());
    }
}
