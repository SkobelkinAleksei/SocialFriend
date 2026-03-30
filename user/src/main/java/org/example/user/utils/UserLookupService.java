package org.example.user.utils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.entity.UserEntity;
import org.example.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLookupService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserEntity getById(Long userId) {
        if (isNull(userId)) {
            throw new NullPointerException("UserId: " + userId + " не может быть null!");
        }

        return userRepository.findById(userId).orElseThrow(
                () -> {
                    log.warn("[ERROR] User с id: {} не найден", userId);
                    return new EntityNotFoundException("Такой пользователь не был найден.");
                });
    }
}