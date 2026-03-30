package org.example.user.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.dto.*;
import org.example.user.entity.UserEntity;
import org.example.user.mapper.UserMapper;
import org.example.user.repository.UserRepository;
import org.example.user.utils.UserLookupService;
import org.example.user.utils.UserSpecification;
import org.example.user.utils.UserUpdateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserLookupService userLookupService;
    private final UserUpdateService userUpdateService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto signUp(RegistrationUserDto registrationUserDto) {
        log.info("[INFO] Создание пользователя с email: {} и номером телефона: {}",
                registrationUserDto.getEmail(), registrationUserDto.getNumberPhone());

        boolean existByEmailOrNumberPhone = userRepository.isExistByEmailOrNumberPhone(
                registrationUserDto.getEmail(),
                registrationUserDto.getNumberPhone()
        );

        if (existByEmailOrNumberPhone) {
            log.warn("[INFO] Попытка создания пользователя с уже существующими email или телефоном: email={}, phone={}",
                    registrationUserDto.getEmail(), registrationUserDto.getNumberPhone());

            throw new IllegalArgumentException("Email или телефон уже используются!");
        }

        UserEntity userEntity = userMapper.toEntity(registrationUserDto);
        userEntity.setPassword(passwordEncoder.encode(registrationUserDto.getPassword()));
        userEntity.setTimeStamp(LocalDateTime.now());

        UserDto userDto = userMapper.toDto(userRepository.save(userEntity));

        log.info("[INFO] Пользователь успешно создан с id: {}", userDto.getUserId());
        return userDto;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUserById(Long userId) {
        log.info("[INFO] Получение пользователя по id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);

        UserDto userDto = userMapper.toDto(userEntity);
        log.info("[INFO] Пользователь: {} успешно получен", userDto);

        return userDto;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUserByEmail(String email) {
        log.info("[INFO] Поиск пользователя по email: {}", email);
        return userMapper.toDto(
                userRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> {
                            log.warn("[INFO] Пользователь с email: {} не найден", email);
                            return new EntityNotFoundException("Пользователь не был найден");
                        }));
    }

    @Transactional(readOnly = true)
    @Override
    public UserFullDto getMyProfile(Long userId) {
        log.info("[INFO] Получение профиля для пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);

        UserFullDto userFullDto = userMapper.toFullDto(userEntity);
        log.info("[INFO] Профиль пользователя: {} успешно получен", userFullDto);

        return userFullDto;
    }

    @Override
    public UserDto updateUserAccount(Long userId, UpdateUserDto updateAccountUser) {
        log.info("[INFO] Обновление аккаунта пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);

        UserDto userDto = userUpdateService.updateAccount(userEntity, updateAccountUser);
        log.info("[INFO] Аккаунт пользователя: {} успешно обновлён", userDto);

        return userDto;
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordUserDto updatePasswordUserDto) {
        log.info("[INFO] Обновление пароля пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);

        userUpdateService.updatePassword(userEntity, updatePasswordUserDto);
        log.info("[INFO] Пароль пользователя с id: {} успешно обновлён", userId);
        userRepository.save(userEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> searchUsers(UserFilterDto filter, int page, int size) {
        log.info("[INFO] Поиск пользователей с фильтром: {}, страница: {}, размер: {}",
                filter, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<UserEntity> users = userRepository.findAll(
                UserSpecification.filter(filter),
                pageable
        );

        List<UserDto> dtoList = users.map(userMapper::toDto).toList();
        log.info("[INFO] Поиск пользователей завершён. Найдено записей: {}", dtoList.size());

        return dtoList;
    }
}
