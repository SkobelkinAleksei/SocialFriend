package org.example.user.service;

import org.example.user.entity.EmailOtpEntity;
import org.example.user.entity.EmailOtpPurpose;
import org.example.user.entity.UserEntity;
import org.example.user.exception.RateLimitedException;
import org.example.user.outbox.OutboxService;
import org.example.user.repository.EmailOtpRepository;
import org.example.user.repository.UserRepository;
import org.example.user.UserFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailOtpService — коды на почту")
class EmailOtpServiceTest {

    @Mock
    private EmailOtpRepository otpRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NeighborhoodMailService mailService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private EmailOtpService service;

    @Test
    @DisplayName("Нет такого email — forgot-password молчит, письмо не шлём")
    void forgotUnknownEmailSilent() {
        when(userRepository.findByEmailIgnoreCase("ghost@ex.ru")).thenReturn(Optional.empty());

        service.sendResetCode("ghost@ex.ru");

        verify(mailService, never()).sendCode(any(), any(), any(), any());
        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Повтор кода раньше минуты — 429")
    void resendTooSoon() {
        UserEntity user = UserFixtures.user(2L);
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("anna@example.com")).thenReturn(Optional.of(user));
        EmailOtpEntity last = new EmailOtpEntity();
        last.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        when(otpRepository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq("anna@example.com"), eq(EmailOtpPurpose.VERIFY))).thenReturn(Optional.of(last));

        assertThrows(RateLimitedException.class, () -> service.sendVerifyCode("anna@example.com"));
        verify(mailService, never()).sendCode(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Первый код: сохраняем хеш и вызываем почту")
    void firstVerifySends() {
        UserEntity user = UserFixtures.user(2L);
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("anna@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$hash");

        service.sendVerifyCode("Anna@Example.com");

        ArgumentCaptor<EmailOtpEntity> captor = ArgumentCaptor.forClass(EmailOtpEntity.class);
        verify(otpRepository).save(captor.capture());
        assertTrue(captor.getValue().getCodeHash().startsWith("$"));
        verify(mailService).sendCode(eq("anna@example.com"), any(), any(), any());
    }

    @Test
    @DisplayName("Почта уже подтверждена — письмо не шлём и ошибку не бросаем")
    void alreadyVerifiedSilent() {
        UserEntity user = UserFixtures.user(2L);
        user.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase("anna@example.com")).thenReturn(Optional.of(user));

        service.sendVerifyCode("anna@example.com");

        verify(mailService, never()).sendCode(any(), any(), any(), any());
        verify(otpRepository, never()).save(any());
    }
}
