package org.example.user.service;

import com.example.common.dto.event.UserDto;
import com.example.common.dto.BlockStatusDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.common.kafka.UserAccountStatusChangedEvent;
import com.example.common.kafka.UserEmailUpdatedEvent;
import com.example.common.kafka.UserPasswordUpdatedEvent;
import com.example.common.kafka.UserRegisteredEvent;
import org.example.restclient.config.IHttpCore;
import org.example.user.client.FriendBlockClient;
import org.example.user.dto.*;
import org.example.user.entity.AccountStatus;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.PlatformRole;
import org.example.user.entity.UserEntity;
import org.example.user.entity.UserSettingsEntity;
import org.example.user.exception.ForbiddenException;
import org.example.user.mapper.UserMapper;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import com.example.common.metrics.AppMetrics;
import org.example.user.repository.UserRepository;
import org.example.user.repository.UserSettingsRepository;
import org.example.user.utils.UserLookupService;
import org.example.user.utils.UserSpecification;
import org.example.user.utils.UserUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int ONLINE_WINDOW_SECONDS = 90;

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final UserMapper userMapper;
    private final UserLookupService userLookupService;
    private final UserUpdateService userUpdateService;
    private final IHttpCore httpCore;
    private final FriendBlockClient friendBlockClient;

    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final AppMetrics appMetrics;

    @Value("${app.services.friend-base-url:http://localhost:8082}")
    private String friendBaseUrl;

    @Override
    @Transactional
    public UserDto signUp(RegistrationUserDto registrationUserDto) {
        log.info("[UserServiceImpl - INFO] Создание пользователя с email: {} и номером телефона: {}",
                registrationUserDto.getEmail(), registrationUserDto.getNumberPhone());

        if (!Boolean.TRUE.equals(registrationUserDto.getAcceptedTerms())) {
            appMetrics.registraciyaOshibka();
            throw new IllegalArgumentException(
                    "Чтобы создать аккаунт, примите Правила сообщества и Политику конфиденциальности.");
        }

        boolean existByEmailOrNumberPhone = userRepository.isExistByEmailOrNumberPhone(
                registrationUserDto.getEmail(),
                registrationUserDto.getNumberPhone()
        );
        if (existByEmailOrNumberPhone) {
            appMetrics.registraciyaOshibka();
            log.warn("[Регистрация] Email или телефон уже используются: {}", registrationUserDto.getEmail());
            throw new IllegalStateException("Email или телефон уже используются!");
        }

        // 2. Маппим пользователя и ставим пароль/био
        UserEntity userEntity = userMapper.toEntity(registrationUserDto);
        userEntity.setPassword(passwordEncoder.encode(registrationUserDto.getPassword()));
        userEntity.setBio("Привет! Давайте знакомиться и двигаться вместе!");
        userEntity.setAccountStatus(AccountStatus.ACTIVE);
        userEntity.setPlatformRole(PlatformRole.USER);
        userEntity.setTermsAcceptedAt(LocalDateTime.now());
        userEntity.setTermsVersion("2026-08-17");
        userEntity.setEmailVerified(false);

        // 3. Создаем настройки, привязывая их к нашему объекту userEntity
        UserSettingsEntity defaultSettings = UserSettingsEntity.builder()
                .user(userEntity) // Связь в одну сторону
                .searchRadius(1.0)
                .allowDmFromAll(true)
                .allowCommentsFromAll(true)
                .photoVisibility(PhotoVisibility.ALL)
                .notifyComments(true)
                .notifyMessages(true)
                .notifyEventRequests(true)
                .notifyReputation(true)
                .showLastSeen(true)
                .build();

        // 4. Устанавливаем двустороннюю связ
        userEntity.setSettings(defaultSettings);

        // 5. Сохраняем ТОЛЬКО пользователя. Настройки сохранятся автоматически!
        // Race: два параллельных signUp могут пройти pre-check — ловим unique constraint.
        final UserEntity saved;
        try {
            saved = userRepository.saveAndFlush(userEntity);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            appMetrics.registraciyaOshibka();
            log.warn("[Регистрация] Конфликт уникальности email/телефона");
            throw new IllegalStateException("Email или телефон уже используются!", e);
        }

        log.info("[UserServiceImpl - INFO] Дефолтные настройки для userId: {} успешно инициализированы", saved.getId());

        UserDto userDto = userMapper.toDto(saved);
        UserRegisteredEvent event = new UserRegisteredEvent(
                saved.getId(),
                saved.getEmail(),
                saved.getPassword(),
                saved.getTimeStamp(),
                saved.effectivePlatformRole().name(),
                false
        );

        outboxService.enqueue(UserKafkaTopics.REGISTERED, saved.getId(), event);
        appMetrics.registraciyaUspeh();
        log.info("[Регистрация] Новый пользователь userId={}", saved.getId());
        return userDto;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUserById(Long userId) {
        log.info("[UserServiceImpl - INFO] Поиск пользователя по userId: {}", userId);
        UserEntity entity = userLookupService.getById(userId);
        return withOnline(entity, applyAccountVisibility(userMapper.toPublicDto(entity), entity));
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto searchUserByEmail(String email) {
        log.info("[UserServiceImpl - INFO] Поиск пользователя по email: {}", email);
        UserEntity entity = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("[INFO] Пользователь с email: {} не найден", email);
                    return new EntityNotFoundException("Пользователь не был найден");
                });
        if (!entity.isActiveAccount()) {
            throw new EntityNotFoundException("Пользователь не был найден");
        }
        return withOnline(entity, userMapper.toPublicDto(entity));
    }

    @Transactional(readOnly = true)
    @Override
    public UserFullDto getMyProfile(Long userId) {
        log.info("[UserServiceImpl - INFO] Получение профиля для пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);
        requireActive(userEntity);

        UserFullDto userFullDto = userMapper.toFullDto(userEntity);
        UserSettingsEntity mySettings = userEntity.getSettings();
        userFullDto.setPhotoVisibility(mySettings == null || mySettings.getPhotoVisibility() == null
                ? PhotoVisibility.ALL.name()
                : mySettings.getPhotoVisibility().name());
        userFullDto.setOnline(isOnlineNow(userEntity.getLastSeenAt()));
        log.info("[UserServiceImpl - INFO] Профиль пользователя: {} успешно получен", userFullDto);

        return userFullDto;
    }

    @Override
    @Transactional
    public UserDto updateUserAccount(Long userId, UpdateUserDto updateAccountUser) {
        log.info("[UserServiceImpl - INFO] Обновление аккаунта пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);
        requireActive(userEntity);

        String newEmail = updateAccountUser.getEmail();
        if (newEmail != null && !newEmail.isBlank()
                && !newEmail.equalsIgnoreCase(userEntity.getEmail())
                && userRepository.existsEmailForOtherUser(newEmail, userId)) {
            throw new IllegalStateException("Email уже используется!");
        }

        String newPhone = updateAccountUser.getNumberPhone();
        if (newPhone != null && !newPhone.isBlank()
                && !newPhone.equals(userEntity.getNumberPhone())
                && userRepository.existsPhoneForOtherUser(newPhone, userId)) {
            throw new IllegalStateException("Телефон уже используется!");
        }

        boolean emailChanged = newEmail != null
                && !newEmail.isBlank()
                && !userEntity.getEmail().equalsIgnoreCase(newEmail);

        final UserDto userDto;
        try {
            userDto = userUpdateService.updateAccount(userEntity, updateAccountUser);
            userRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalStateException("Email или телефон уже используются!", e);
        }
        log.info("[UserServiceImpl - INFO] Аккаунт пользователя: {} успешно обновлён", userDto);

        if (emailChanged) {
            log.info("[UserServiceImpl - INFO] Почта изменена. Событие в outbox для userId: {}", userId);
            outboxService.enqueue(
                    UserKafkaTopics.EMAIL_UPDATED,
                    userId,
                    new UserEmailUpdatedEvent(userId, userEntity.getEmail())
            );
        }

        return withOnline(userEntity, userDto);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordUserDto updatePasswordUserDto) {
        log.info("[UserServiceImpl - INFO] Обновление пароля пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);
        requireActive(userEntity);

        userUpdateService.updatePassword(userEntity, updatePasswordUserDto);
        log.info("[UserServiceImpl - INFO] Пароль пользователя с id: {} успешно обновлён", userId);
        userRepository.save(userEntity);

        outboxService.enqueue(
                UserKafkaTopics.PASSWORD_UPDATED,
                userId,
                new UserPasswordUpdatedEvent(userId, userEntity.getPassword())
        );
        log.info("[UserServiceImpl - INFO] Событие о смене пароля записано в outbox для userId: {}", userId);
    }

    @Override
    @Transactional
    public void updateBio(Long userId, UpdateBioDto newBio) {
        log.info("[UserServiceImpl - INFO] Обновление статуса (bio) для пользователя с id: {}", userId);
        UserEntity userEntity = userLookupService.getById(userId);
        requireActive(userEntity);

        userEntity.setBio(newBio.getBio());

        userRepository.save(userEntity);
        log.info("[UserServiceImpl - INFO] Статус пользователя с id: {} успешно сохранен в базе данных", userId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> searchUsers(Long currentUserId, UserFilterDto filter, int page, int size) {
        log.info("[UserServiceImpl - INFO] Поиск пользователей с фильтром: {}, страница: {}, размер: {}",
                filter, page, size);
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 20);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());

        Page<UserEntity> users = userRepository.findAll(
                UserSpecification.filter(filter, currentUserId, friendBlockClient.hiddenUserIds(currentUserId)),
                pageable
        );

        return users.stream().map(userEntity -> {
            UserDto dto = userMapper.toPublicDto(userEntity);
            UserSettingsEntity settings = userEntity.getSettings();

            if (settings != null) {
                dto.setCanMessage(settings.getAllowDmFromAll());
                dto.setCanComment(settings.getAllowCommentsFromAll());
            } else {
                dto.setCanMessage(true);
                dto.setCanComment(true);
            }
            return withOnline(userEntity, dto);
        }).toList();
    }

    @Override
    @Transactional
    public void updateReputation(Long userId, long change) {
        log.info("[UserService] Обновление репутации пользователя {}. Изменение: {}", userId, change);

        int rowsUpdated = userRepository.updateReputation(userId, change);

        if (rowsUpdated == 0) {
            throw new EntityNotFoundException(
                    "Пользователь не найден при обновлении репутации: " + userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserProfileForViewer(Long userId, Long viewerId) {
        log.info("[UserServiceImpl] Запрос профиля пользователя {} для зрителя {}", userId, viewerId);

        UserEntity targetUser = userLookupService.getById(userId);
        UserDto dto = applyAccountVisibility(userMapper.toPublicDto(targetUser), targetUser);
        withOnline(targetUser, dto);

        if (!targetUser.isActiveAccount()) {
            dto.setCanMessage(false);
            dto.setCanComment(false);
            dto.setCanSeePhotos(false);
            return dto;
        }

        if (userId.equals(viewerId)) {
            dto.setCanMessage(true);
            dto.setCanComment(true);
            applyPhotoAccess(dto, targetUser, true);
            dto.setCanSeePhotos(true);
            return dto;
        }

        BlockStatusDto block = friendBlockClient.status(viewerId, userId);
        dto.setBlockedByMe(block.isBlockedByMe());
        dto.setBlockedMe(block.isBlockedMe());
        if (block.isEitherWay()) {
            dto.setCanMessage(false);
            dto.setCanComment(false);
            applyPhotoAccess(dto, targetUser, false);
            return dto;
        }

        boolean areFriends = checkFriendshipWithService(viewerId, userId);
        UserSettingsEntity settings = targetUser.getSettings();
        if (settings != null) {
            dto.setCanMessage(settings.getAllowDmFromAll() || areFriends);
            dto.setCanComment(settings.getAllowCommentsFromAll() || areFriends);
        } else {
            dto.setCanMessage(true);
            dto.setCanComment(true);
        }
        applyPhotoAccess(dto, targetUser, areFriends);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.debug("[UserServiceImpl] Извлечение пользователей для списка ID: {}", ids);

        List<UserEntity> users = userRepository.findAllById(ids);

        return users.stream()
                .map(userEntity -> withOnline(userEntity, applyAccountVisibility(userMapper.toPublicDto(userEntity), userEntity)))
                .toList();
    }

    @Override
    @Transactional
    public void heartbeat(Long userId) {
        UserEntity user = userLookupService.getById(userId);
        if (!user.isActiveAccount()) {
            return;
        }
        userRepository.touchLastSeen(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteMyAccount(Long userId) {
        UserEntity user = userLookupService.getById(userId);
        if (user.effectiveAccountStatus() == AccountStatus.DELETED) {
            return;
        }
        user.setAccountStatus(AccountStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        enqueueStatus(userId, AccountStatus.DELETED);
        log.info("[User] Мягкое удаление аккаунта userId={}", userId);
    }

    @Override
    @Transactional
    public void banUser(Long userId) {
        UserEntity user = userLookupService.getById(userId);
        if (user.isPlatformAdmin()) {
            throw new ForbiddenException("Нельзя забанить администратора");
        }
        if (user.effectiveAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalStateException("Нельзя забанить удалённый аккаунт");
        }
        if (user.effectiveAccountStatus() == AccountStatus.BANNED) {
            return;
        }
        user.setAccountStatus(AccountStatus.BANNED);
        user.setBannedAt(LocalDateTime.now());
        userRepository.save(user);
        enqueueStatus(userId, AccountStatus.BANNED);
        log.info("[User] Бан аккаунта userId={}", userId);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId) {
        UserEntity user = userLookupService.getById(userId);
        if (user.effectiveAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalStateException("Удалённый аккаунт нельзя разбанить");
        }
        if (user.isActiveAccount()) {
            return;
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setBannedAt(null);
        userRepository.save(user);
        enqueueStatus(userId, AccountStatus.ACTIVE);
        log.info("[User] Разбан аккаунта userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String accountStatus(Long userId) {
        return userLookupService.getById(userId).effectiveAccountStatus().name();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserPresenceDto> getPresence(List<Long> ids) {
        Map<Long, UserPresenceDto> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        List<UserEntity> users = userRepository.findAllById(ids);
        for (UserEntity user : users) {
            result.put(user.getId(), toPresence(user));
        }
        for (Long id : ids) {
            result.putIfAbsent(id, new UserPresenceDto(false, null));
        }
        return result;
    }

    private boolean isOnlineNow(LocalDateTime lastSeenAt) {
        return lastSeenAt != null && lastSeenAt.isAfter(LocalDateTime.now().minusSeconds(ONLINE_WINDOW_SECONDS));
    }

    private boolean allowsLastSeen(UserEntity entity) {
        UserSettingsEntity settings = entity.getSettings();
        return settings == null || settings.getShowLastSeen() == null || Boolean.TRUE.equals(settings.getShowLastSeen());
    }

    private UserPresenceDto toPresence(UserEntity entity) {
        boolean show = allowsLastSeen(entity);
        boolean online = isOnlineNow(entity.getLastSeenAt());
        return new UserPresenceDto(show && online, show ? entity.getLastSeenAt() : null);
    }

    private UserDto withOnline(UserEntity entity, UserDto dto) {
        boolean show = allowsLastSeen(entity);
        dto.setOnline(show && isOnlineNow(entity.getLastSeenAt()));
        dto.setLastSeenAt(show ? entity.getLastSeenAt() : null);
        return dto;
    }

    private void applyPhotoAccess(UserDto dto, UserEntity target, boolean areFriends) {
        UserSettingsEntity settings = target.getSettings();
        PhotoVisibility visibility = settings == null || settings.getPhotoVisibility() == null
                ? PhotoVisibility.ALL
                : settings.getPhotoVisibility();
        dto.setPhotoVisibility(visibility.name());
        dto.setCanSeePhotos(visibility == PhotoVisibility.ALL
                || (visibility == PhotoVisibility.FRIENDS && areFriends));
    }

    private void requireActive(UserEntity user) {
        if (!user.isActiveAccount()) {
            throw new ForbiddenException("Аккаунт недоступен");
        }
    }

    private void enqueueStatus(Long userId, AccountStatus status) {
        outboxService.enqueue(
                UserKafkaTopics.ACCOUNT_STATUS_CHANGED,
                userId,
                new UserAccountStatusChangedEvent(userId, status.name())
        );
    }

    private UserDto applyAccountVisibility(UserDto dto, UserEntity entity) {
        AccountStatus status = entity.effectiveAccountStatus();
        dto.setAccountStatus(status.name());
        if (status == AccountStatus.DELETED) {
            dto.setFirstName("Аккаунт");
            dto.setLastName("удалён");
            dto.setBio(null);
            dto.setAvatarUrl(null);
            dto.setCoverMode(null);
            dto.setCoverColor(null);
            dto.setCoverUrl(null);
            dto.setCity("");
            dto.setDistrictName("");
            dto.setReputation(0L);
            dto.setOnline(false);
            dto.setLastSeenAt(null);
            dto.setCanMessage(false);
            dto.setCanComment(false);
            dto.setCanSeePhotos(false);
        } else if (status == AccountStatus.BANNED) {
            dto.setFirstName("Профиль");
            dto.setLastName("недоступен");
            dto.setBio(null);
            dto.setAvatarUrl(null);
            dto.setCoverMode(null);
            dto.setCoverColor(null);
            dto.setCoverUrl(null);
            dto.setCity("");
            dto.setDistrictName("");
            dto.setOnline(false);
            dto.setLastSeenAt(null);
            dto.setCanMessage(false);
            dto.setCanComment(false);
            dto.setCanSeePhotos(false);
        }
        return dto;
    }

    private boolean checkFriendshipWithService(Long viewerId, Long userId) {
        try {
            // Прямой вызов friend-сервиса (без gateway JWT)
            String friendUrl = friendBaseUrl + "/api/v1/social/friends/public/check?userId1="
                    + viewerId + "&userId2=" + userId;
            com.example.common.RequestData requestData = new com.example.common.RequestData(friendUrl);
            org.springframework.http.ResponseEntity<Boolean> response = httpCore.get(requestData, Boolean.class);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("[httpCore] Не удалось проверить статус дружбы", e);
        }
        return false;
    }

}