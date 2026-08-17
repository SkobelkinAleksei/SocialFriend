package org.example.post.service.admin;

import com.example.common.kafka.UserAccountStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountStatusListener {

    private final AdminPostService adminPostService;

    @KafkaListener(
            topics = "user-account-status-changed",
            groupId = "post-admin-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountStatus(UserAccountStatusChangedEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        String status = event.getStatus() == null ? "" : event.getStatus().trim().toUpperCase();
        if ("BANNED".equals(status) || "DELETED".equals(status)) {
            adminPostService.hidePublishedForBan(event.getUserId());
        } else if ("ACTIVE".equals(status)) {
            adminPostService.restoreHiddenForBan(event.getUserId());
        }
    }
}
