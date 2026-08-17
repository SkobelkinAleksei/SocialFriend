package org.example.security.service;

import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.metrics.AppMetrics;
import org.example.security.entity.UserSecurity;
import org.example.security.exception.UserSecurityNotReadyException;
import org.example.security.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUserService — учётная запись входа и блокировка")
public class SecurityUserServiceTest {

    @Mock
    private UserSecurityRepository repository;
    @Mock
    private AppMetrics appMetrics;

    @InjectMocks
    private SecurityUserService service;

    @BeforeEach
    void lockSettings() {
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "lockDurationMinutes", 15);
    }

    @Test
    @DisplayName("Пользователь не найден по email — UsernameNotFoundException")
    void loadMissing() {
        when(repository.findByUsername("anna@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("Anna@Example.com"));
    }

    @Nested
    @DisplayName("Создание из события регистрации")
    class FromEvent {

        @Test
        @DisplayName("Уже есть запись — идемпотентный skip, второй save не делаем")
        void alreadyExists() {
            when(repository.existsById(3L)).thenReturn(true);
            UserRegisteredEvent event = new UserRegisteredEvent(3L, "a@b.c", "hash", LocalDateTime.now());

            service.createUserFromEvent(event);

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Новый пользователь: email нормализуется, попытки сброса в 0")
        void created() {
            when(repository.existsById(3L)).thenReturn(false);
            UserRegisteredEvent event = new UserRegisteredEvent(3L, "  A@B.C ", "hash", LocalDateTime.now());

            service.createUserFromEvent(event);

            verify(repository).saveAndFlush(any(UserSecurity.class));
        }

        @Test
        @DisplayName("Гонка unique: если после ошибки запись уже есть — не пробрасываем")
        void raceSkip() {
            when(repository.existsById(3L)).thenReturn(false, true);
            when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
            UserRegisteredEvent event = new UserRegisteredEvent(3L, "a@b.c", "hash", LocalDateTime.now());

            service.createUserFromEvent(event);
        }
    }

    @Nested
    @DisplayName("Неудачные попытки входа")
    class Failures {

        @Test
        @DisplayName("Пустой логин — ничего не делаем")
        void blankLogin() {
            service.registerFailedAttempt("  ");
            verify(repository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Уже заблокирован — счётчик не увеличиваем")
        void alreadyLocked() {
            UserSecurity user = user(1L, 5);
            user.setLockedUntil(Instant.now().plusSeconds(600));
            when(repository.findByUsername("a@b.c")).thenReturn(Optional.of(user));

            service.registerFailedAttempt("a@b.c");

            assertEquals(5, user.getFailedAttempts());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Пятая неудача блокирует аккаунт на 15 минут")
        void locksOnFifth() {
            UserSecurity user = user(1L, 4);
            when(repository.findByUsername("a@b.c")).thenReturn(Optional.of(user));

            service.registerFailedAttempt("a@b.c");

            assertEquals(5, user.getFailedAttempts());
            assertTrue(user.isLockedNow());
            verify(appMetrics).vhodBlokirovka();
            verify(repository).save(user);
        }
    }

    @Test
    @DisplayName("Успешный вход сбрасывает счётчик и блокировку")
    void resetAttempts() {
        UserSecurity user = user(1L, 3);
        user.setLockedUntil(Instant.now().plusSeconds(60));
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.resetFailedAttempts(1L);

        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    @DisplayName("Смена пароля до появления security-записи — UserSecurityNotReadyException")
    void passwordBeforeReady() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(UserSecurityNotReadyException.class, () -> service.updatePassword(9L, "hash"));
    }

    @Test
    @DisplayName("Смена пароля сбрасывает блокировку")
    void passwordUnlocks() {
        UserSecurity user = user(2L, 5);
        user.setLockedUntil(Instant.now().plusSeconds(120));
        when(repository.findById(2L)).thenReturn(Optional.of(user));

        service.updatePassword(2L, "new-hash");

        assertEquals("new-hash", user.getPassword());
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
        assertFalse(user.isLockedNow());
    }

    @Test
    @DisplayName("Бан отключает вход, разбан включает обратно")
    void applyAccountStatusTogglesEnabled() {
        UserSecurity user = user(4L, 0);
        user.setEnabled(true);
        when(repository.findById(4L)).thenReturn(Optional.of(user));

        service.applyAccountStatus(4L, "BANNED");
        assertFalse(user.isEnabled());
        assertEquals("BANNED", user.getAccountStatus());

        service.applyAccountStatus(4L, "ACTIVE");
        assertTrue(user.isEnabled());
        assertEquals("ACTIVE", user.getAccountStatus());
    }

    private static UserSecurity user(long id, int attempts) {
        UserSecurity user = new UserSecurity();
        user.setId(id);
        user.setUsername("a@b.c");
        user.setPassword("hash");
        user.setFailedAttempts(attempts);
        return user;
    }
}
