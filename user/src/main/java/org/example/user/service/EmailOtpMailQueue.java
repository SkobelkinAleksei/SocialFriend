package org.example.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOtpMailQueue {

    private final EmailOtpService emailOtpService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendVerifyAfterSignup(UserSignedUpEvent event) {
        log.info("[Почта] Регистрация сохранена, отправляем код на {}", event.email());
        try {
            emailOtpService.sendVerifyCode(event.email());
        } catch (Exception ex) {
            log.warn("[Почта] Не отправили код после регистрации на {}: {}",
                    event.email(), ex.getMessage());
        }
    }
}
