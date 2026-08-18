package org.example.user.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailOutboxRelayScheduler {

    private final MailOutboxRelay mailOutboxRelay;

    @Scheduled(fixedDelayString = "${app.mail.outbox.relay-ms:1000}")
    public void relay() {
        mailOutboxRelay.relayDue();
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox.recover-ms:30000}")
    public void recoverStuck() {
        mailOutboxRelay.recoverStuck();
    }
}
