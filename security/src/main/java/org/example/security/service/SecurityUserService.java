package org.example.security.service;

import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.metrics.AppMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.entity.UserSecurity;
import org.example.security.exception.UserSecurityNotReadyException;
import org.example.security.repository.UserSecurityRepository;
import org.example.security.util.LoginNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserService implements UserDetailsService {

    private final UserSecurityRepository repository;
    private final AppMetrics appMetrics;

    @Value("${app.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.auth.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = LoginNormalizer.normalize(username);
        UserSecurity user = repository.findByUsername(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        return new SecurityUserDetails(user);
    }

    public String loadAccountStatus(Long userId) {
        return repository.findById(userId).map(UserSecurity::getAccountStatus).orElse(null);
    }

    public String loadAccountStatusByUsername(String username) {
        String login = LoginNormalizer.normalize(username);
        return repository.findByUsername(login).map(UserSecurity::getAccountStatus).orElse(null);
    }

    public UserDetails loadUserById(Long userId) {
        UserSecurity user = repository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден по id: " + userId));
        return new SecurityUserDetails(user);
    }

    @Transactional
    public void createUserFromEvent(UserRegisteredEvent event) {
        log.info("[SecurityUserService] Создание security-записи для userId={}", event.getId());

        if (repository.existsById(event.getId())) {
            log.info("[SecurityUserService] Пользователь {} уже существует — идемпотентный skip", event.getId());
            return;
        }

        UserSecurity userSecurity = new UserSecurity();
        userSecurity.setId(event.getId());
        userSecurity.setUsername(LoginNormalizer.normalize(event.getEmail()));
        userSecurity.setPassword(event.getPasswordHash());
        userSecurity.setFailedAttempts(0);
        userSecurity.setEnabled(true);
        userSecurity.setAccountStatus("ACTIVE");
        userSecurity.setPlatformRole("ADMIN".equalsIgnoreCase(event.getPlatformRole()) ? "ADMIN" : "USER");
        userSecurity.setEmailVerified(event.emailVerifiedOrLegacy());
        try {
            repository.saveAndFlush(userSecurity);
        } catch (DataIntegrityViolationException e) {
            if (repository.existsById(event.getId())) {
                log.info("[SecurityUserService] Пользователь {} создан параллельно — skip", event.getId());
                return;
            }
            throw e;
        }
        log.info("[SecurityUserService] Security-запись создана для userId={}", event.getId());
    }

    @Transactional
    public void updateEmail(Long userId, String newEmail) {
        log.info("[SecurityUserService] Обновление login/email для userId={}", userId);
        UserSecurity user = requireUser(userId);
        user.setUsername(LoginNormalizer.normalize(newEmail));
        repository.save(user);
    }

    @Transactional
    public void updatePassword(Long userId, String passwordHash) {
        log.info("[SecurityUserService] Обновление passwordHash для userId={}", userId);
        UserSecurity user = requireUser(userId);
        user.setPassword(passwordHash);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        repository.save(user);
    }

    @Transactional
    public void applyAccountStatus(Long userId, String status) {
        UserSecurity user = requireUser(userId);
        String normalized = status == null ? "ACTIVE" : status.trim().toUpperCase();
        user.setAccountStatus(normalized);
        user.setEnabled("ACTIVE".equals(normalized));
        repository.save(user);
    }

    @Transactional
    public void applyPlatformRole(Long userId, String role) {
        UserSecurity user = requireUser(userId);
        user.setPlatformRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        repository.save(user);
    }

    @Transactional
    public void markEmailVerified(Long userId) {
        UserSecurity user = requireUser(userId);
        user.setEmailVerified(true);
        repository.save(user);
    }

    public boolean isEmailVerified(Long userId) {
        return repository.findById(userId).map(UserSecurity::isEmailVerified).orElse(true);
    }

    public String loadPlatformRole(Long userId) {
        return repository.findById(userId).map(UserSecurity::effectivePlatformRole).orElse("USER");
    }

    @Transactional
    public void registerFailedAttempt(String username) {
        String login = LoginNormalizer.normalize(username);
        if (login == null || login.isBlank()) {
            return;
        }
        repository.findByUsername(login).ifPresent(user -> {
            Instant now = Instant.now();
            if (user.isLockedNow()) {
                return;
            }
            int attempts = user.getFailedAttempts() + 1;
            if (user.getLockedUntil() != null) {
                attempts = 1;
                user.setLockedUntil(null);
            }
            user.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setLockedUntil(now.plus(Duration.ofMinutes(lockDurationMinutes)));
                appMetrics.vhodBlokirovka();
                log.warn("[Вход] Аккаунт userId={} заблокирован на {} мин после {} неудачных попыток",
                        user.getId(), lockDurationMinutes, attempts);
            }
            repository.save(user);
        });
    }

    @Transactional
    public void resetFailedAttempts(Long userId) {
        repository.findById(userId).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            repository.save(user);
        });
    }

    private UserSecurity requireUser(Long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new UserSecurityNotReadyException("Пользователь ещё не создан в security: " + userId));
    }
}
