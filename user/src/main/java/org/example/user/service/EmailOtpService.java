package org.example.user.service;

import com.example.common.kafka.UserEmailVerifiedEvent;
import com.example.common.kafka.UserPasswordUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.example.user.entity.EmailOtpEntity;
import org.example.user.entity.EmailOtpPurpose;
import org.example.user.entity.UserEntity;
import org.example.user.exception.RateLimitedException;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.EmailOtpRepository;
import org.example.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private static final int TTL_MINUTES = 20;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NeighborhoodMailService mailService;
    private final OutboxService outboxService;

    @Transactional
    public void sendVerifyCode(String rawEmail) {
        String email = normalize(rawEmail);
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActiveAccount() || Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        issue(email, EmailOtpPurpose.VERIFY,
                "Код подтверждения — На районе",
                "Ваш код подтверждения почты в «На районе»: %s\n\nДействует 20 минут. Если это не вы — просто удалите письмо.");
    }

    @Transactional
    public void sendResetCode(String rawEmail) {
        String email = normalize(rawEmail);
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActiveAccount()) {
            return;
        }
        issue(email, EmailOtpPurpose.RESET,
                "Сброс пароля — На районе",
                "Код для нового пароля в «На районе»: %s\n\nДействует 20 минут. Если это не вы — удалите письмо, пароль не изменится.");
    }

    @Transactional
    public void verifyEmail(String rawEmail, String code) {
        String email = normalize(rawEmail);
        UserEntity user = requireActiveUser(email);
        consume(email, EmailOtpPurpose.VERIFY, code);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
            outboxService.enqueue(
                    UserKafkaTopics.EMAIL_VERIFIED,
                    user.getId(),
                    new UserEmailVerifiedEvent(user.getId())
            );
        }
    }

    @Transactional
    public void resetPassword(String rawEmail, String code, String newPassword) {
        String email = normalize(rawEmail);
        UserEntity user = requireActiveUser(email);
        consume(email, EmailOtpPurpose.RESET, code);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        userRepository.save(user);
        outboxService.enqueue(
                UserKafkaTopics.PASSWORD_UPDATED,
                user.getId(),
                new UserPasswordUpdatedEvent(user.getId(), user.getPassword())
        );
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            outboxService.enqueue(
                    UserKafkaTopics.EMAIL_VERIFIED,
                    user.getId(),
                    new UserEmailVerifiedEvent(user.getId())
            );
        }
    }

    private void issue(String email, EmailOtpPurpose purpose, String subject, String bodyTemplate) {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .ifPresent(last -> {
                    if (last.getCreatedAt() != null && last.getCreatedAt().isAfter(now.minusSeconds(RESEND_SECONDS))) {
                        throw new RateLimitedException("Код уже отправили. Подождите минуту и запросите снова.");
                    }
                });
        otpRepository.expireOpen(email, purpose, now);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailOtpEntity otp = new EmailOtpEntity();
        otp.setEmail(email);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusMinutes(TTL_MINUTES));
        otp.setAttempts(0);
        otpRepository.save(otp);
        mailService.sendCode(email, subject, bodyTemplate.formatted(code), code);
    }

    private void consume(String email, EmailOtpPurpose purpose, String code) {
        EmailOtpEntity otp = otpRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Неверный или просроченный код"));
        LocalDateTime now = LocalDateTime.now();
        if (otp.getExpiresAt() == null || otp.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Неверный или просроченный код");
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setUsedAt(now);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Слишком много попыток. Запросите новый код.");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Неверный или просроченный код");
        }
        otp.setUsedAt(now);
        otpRepository.save(otp);
    }

    private UserEntity requireActiveUser(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Неверный или просроченный код"));
        if (!user.isActiveAccount()) {
            throw new IllegalArgumentException("Неверный или просроченный код");
        }
        return user;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
