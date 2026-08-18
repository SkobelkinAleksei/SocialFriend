package org.example.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpMailQueue {

    private final EmailOtpService emailOtpService;

    @Async
    public void sendVerifyAfterSignup(String email) {
        try {
            emailOtpService.sendVerifyCode(email);
        } catch (Exception ex) {
            log.warn("[Почта] Фоновая отправка после регистрации не удалась на {}: {}",
                    email, ex.getMessage());
        }
    }
}
