package org.example.user.service.admin;

import com.example.common.kafka.UserPlatformRoleChangedEvent;
import com.example.common.kafka.UserRegisteredEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.admin.AdminPersonDto;
import org.example.user.dto.admin.CreateAdminRequest;
import org.example.user.entity.AccountStatus;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.PlatformRole;
import org.example.user.entity.UserEntity;
import org.example.user.entity.UserSettingsEntity;
import org.example.user.exception.ForbiddenException;
import org.example.user.mapper.UserMapper;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserRepository;
import org.example.user.validation.UserValidationRules;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStaffService {

    private static final Pattern NAME = Pattern.compile(UserValidationRules.NAME);
    private static final Pattern PHONE = Pattern.compile(UserValidationRules.PHONE);
    private static final Pattern PASSWORD = Pattern.compile(UserValidationRules.PASSWORD);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public List<AdminPersonDto> list() {
        return userRepository.findAllByPlatformRoleOrderByIdAsc(PlatformRole.ADMIN).stream()
                .map(AdminStaffService::toDto)
                .toList();
    }

    @Transactional
    public AdminPersonDto createOrGrant(CreateAdminRequest request, Long actorId) {
        if (request == null) {
            throw new IllegalArgumentException("Нужны данные админа");
        }
        if (request.getUserId() != null) {
            return grant(request.getUserId(), actorId);
        }
        return createAccount(request, actorId);
    }

    @Transactional
    public void revoke(Long userId, Long actorId) {
        if (userId == null) {
            throw new IllegalArgumentException("Не указан админ");
        }
        if (actorId != null && actorId.equals(userId)) {
            throw new ForbiddenException("Нельзя снять права с самого себя");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Админ не найден"));
        if (!user.isPlatformAdmin()) {
            return;
        }
        if (userRepository.countByPlatformRole(PlatformRole.ADMIN) <= 1) {
            throw new IllegalStateException("Нельзя снять последнего админа");
        }
        applyRole(user, PlatformRole.USER);
        log.info("[Admin] Снята роль ADMIN userId={} actorId={}", userId, actorId);
    }

    private AdminPersonDto grant(Long userId, Long actorId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        if (user.isPlatformAdmin()) {
            return toDto(user);
        }
        if (user.effectiveAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Админом можно сделать только активный аккаунт");
        }
        applyRole(user, PlatformRole.ADMIN);
        log.info("[Admin] Назначена роль ADMIN userId={} actorId={}", userId, actorId);
        return toDto(user);
    }

    private AdminPersonDto createAccount(CreateAdminRequest request, Long actorId) {
        RegistrationUserDto signup = new RegistrationUserDto();
        signup.setFirstName(trim(request.getFirstName()));
        signup.setLastName(trim(request.getLastName()));
        signup.setEmail(request.getEmail());
        signup.setNumberPhone(request.getNumberPhone());
        signup.setPassword(request.getPassword() == null ? null : request.getPassword().trim());
        signup.setBirthday(LocalDate.of(1990, 1, 1));
        signup.setCity("Служебный");
        signup.setStreetAddress("служебный аккаунт");
        signup.setDistrictName("Служебный");
        signup.setHomeLatitude(new BigDecimal("55.751244"));
        signup.setHomeLongitude(new BigDecimal("37.618423"));
        validateNewAccount(signup);
        if (userRepository.isExistByEmailOrNumberPhone(signup.getEmail(), signup.getNumberPhone())) {
            throw new IllegalStateException("Email или телефон уже используются!");
        }
        UserEntity userEntity = userMapper.toEntity(signup);
        userEntity.setPassword(passwordEncoder.encode(signup.getPassword()));
        userEntity.setBio("Админ района");
        userEntity.setAccountStatus(AccountStatus.ACTIVE);
        userEntity.setPlatformRole(PlatformRole.ADMIN);
        UserSettingsEntity settings = UserSettingsEntity.builder()
                .user(userEntity)
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
        userEntity.setSettings(settings);
        UserEntity saved;
        try {
            saved = userRepository.saveAndFlush(userEntity);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalStateException("Email или телефон уже используются!", e);
        }
        outboxService.enqueue(
                UserKafkaTopics.REGISTERED,
                saved.getId(),
                new UserRegisteredEvent(
                        saved.getId(),
                        saved.getEmail(),
                        saved.getPassword(),
                        saved.getTimeStamp(),
                        PlatformRole.ADMIN.name()
                )
        );
        log.info("[Admin] Создан админ userId={} email={} actorId={}", saved.getId(), saved.getEmail(), actorId);
        return toDto(saved);
    }

    private void applyRole(UserEntity user, PlatformRole role) {
        user.setPlatformRole(role);
        userRepository.save(user);
        outboxService.enqueue(
                UserKafkaTopics.PLATFORM_ROLE_CHANGED,
                user.getId(),
                new UserPlatformRoleChangedEvent(user.getId(), role.name())
        );
    }

    private static void validateNewAccount(RegistrationUserDto signup) {
        if (signup.getFirstName() == null || !NAME.matcher(signup.getFirstName()).matches()) {
            throw new IllegalArgumentException(UserValidationRules.NAME_MESSAGE);
        }
        if (signup.getLastName() == null || !NAME.matcher(signup.getLastName()).matches()) {
            throw new IllegalArgumentException(UserValidationRules.NAME_MESSAGE);
        }
        if (signup.getEmail() == null || signup.getEmail().isBlank() || !signup.getEmail().contains("@")) {
            throw new IllegalArgumentException("Введите корректный email");
        }
        if (signup.getNumberPhone() == null || !PHONE.matcher(signup.getNumberPhone()).matches()) {
            throw new IllegalArgumentException(UserValidationRules.PHONE_MESSAGE);
        }
        if (signup.getPassword() == null || !PASSWORD.matcher(signup.getPassword()).matches()) {
            throw new IllegalArgumentException(UserValidationRules.PASSWORD_MESSAGE);
        }
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static AdminPersonDto toDto(UserEntity user) {
        return AdminPersonDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail() == null ? null : user.getEmail().toLowerCase(Locale.ROOT))
                .accountStatus(user.effectiveAccountStatus().name())
                .platformRole(user.effectivePlatformRole().name())
                .reportsTotal(0)
                .reportsUpheld(0)
                .build();
    }
}
