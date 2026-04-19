package org.example.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.common.kafka.UserRegisteredEvent;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.entity.UserSecurity;
import org.example.security.repository.UserSecurityRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserService implements UserDetailsService {

    private final UserSecurityRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[SecurityUserService - INFO] Поиск пользователя по username: {}", username);
        UserSecurity user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Такой пользователь не был найден: " + username));

        return new SecurityUserDetails(user);
    }

    public void createUserFromEvent(UserRegisteredEvent event) {
        log.info("[SecurityUserService - INFO] Создание новой записи пользователя ID: {}", event.getId());
        UserSecurity userSecurity = new UserSecurity();
        userSecurity.setId(event.getId());
        userSecurity.setUsername(event.getUsername());
        userSecurity.setPassword(event.getPasswordHash());

        repository.save(userSecurity);
        log.info("[SecurityUserService - INFO] Запись успешно создана для пользователя ID: {}", event.getId());
    }

    @Transactional
    public void updateEmail(Long userId, String newEmail) {
        log.info("[SecurityUserService - INFO] Обновление почты (username) для пользователя ID: {}", userId);
        UserSecurity user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId));

        user.setUsername(newEmail);
        repository.save(user);
        log.info("[SecurityUserService - INFO] Почта успешно обновлена для пользователя ID: {}", userId);
    }

    @Transactional
    public void updatePassword(Long userId, String newEncodedPassword) {
        log.info("[SecurityUserService - INFO] Обновление пароля для пользователя ID: {}", userId);
        UserSecurity user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId));

        user.setPassword(newEncodedPassword);
        repository.save(user);
        log.info("[SecurityUserService - INFO] Пароль успешно обновлен для пользователя ID: {}", userId);
    }
}
