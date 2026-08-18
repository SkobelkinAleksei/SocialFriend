package org.example.user.mail;

import org.example.user.service.NeighborhoodMailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailOutboxRelay — отправка после commit")
class MailOutboxRelayTest {

    @Mock
    private MailOutboxRepository mailOutboxRepository;
    @Mock
    private NeighborhoodMailService mailService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private MailOutboxRelay relay;

    @Test
    @DisplayName("Успех: письмо уходит, текст кода из очереди стираем")
    void sentClearsBody() {
        MailOutboxEntity letter = pending(1L);
        letter.setStatus(MailOutboxStatus.SENDING);
        when(mailOutboxRepository.findById(1L)).thenReturn(Optional.of(letter));

        relay.trySendClaimed(1L);

        verify(mailService).sendCode(eq("anna@example.com"), eq("Тема"), eq("Код 123456"), eq(""));
        verify(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("SMTP упал — письмо остаётся в очереди на повтор")
    void smtpFailureRetries() {
        MailOutboxEntity letter = pending(2L);
        letter.setStatus(MailOutboxStatus.SENDING);
        letter.setAttempts(1);
        when(mailOutboxRepository.findById(2L)).thenReturn(Optional.of(letter));
        doThrow(new IllegalStateException("Не получилось отправить письмо. Попробуйте через минуту."))
                .when(mailService).sendCode(anyString(), anyString(), anyString(), anyString());

        relay.trySendClaimed(2L);

        verify(transactionTemplate).executeWithoutResult(any());
        verify(mailOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Код просрочен — не шлём, помечаем DEAD")
    void expiredNotSent() {
        MailOutboxEntity letter = pending(3L);
        letter.setStatus(MailOutboxStatus.SENDING);
        letter.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(mailOutboxRepository.findById(3L)).thenReturn(Optional.of(letter));

        relay.trySendClaimed(3L);

        verify(mailService, never()).sendCode(any(), any(), any(), any());
        verify(transactionTemplate).executeWithoutResult(any());
    }

    private static MailOutboxEntity pending(long id) {
        return MailOutboxEntity.builder()
                .id(id)
                .toEmail("anna@example.com")
                .subject("Тема")
                .body("Код 123456")
                .status(MailOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();
    }
}
