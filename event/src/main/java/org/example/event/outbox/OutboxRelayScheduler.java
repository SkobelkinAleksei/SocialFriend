package org.example.event.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRelay outboxRelay;

    @Scheduled(fixedDelayString = "${app.outbox.relay-ms:1000}")
    public void relay() {
        outboxRelay.relayDue();
    }

    @Scheduled(fixedDelayString = "${app.outbox.recover-ms:30000}")
    public void recoverStuck() {
        outboxRelay.recoverStuck();
    }
}
