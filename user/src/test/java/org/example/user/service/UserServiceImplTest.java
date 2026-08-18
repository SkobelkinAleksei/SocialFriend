package org.example.user.service;

import com.example.common.dto.BlockStatusDto;
import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.UserAccountStatusChangedEvent;
import com.example.common.kafka.UserEmailUpdatedEvent;
import com.example.common.kafka.UserPasswordUpdatedEvent;
import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.metrics.AppMetrics;
import jakarta.persistence.EntityNotFoundException;
import org.example.restclient.config.IHttpCore;
import org.example.user.UserFixtures;
import org.example.user.dto.UpdateBioDto;
import org.example.user.dto.UpdatePasswordUserDto;
import org.example.user.dto.UpdateUserDto;
import org.example.user.dto.UserFullDto;
import org.example.user.dto.UserPresenceDto;
import org.example.user.entity.AccountStatus;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.UserEntity;
import org.example.user.mapper.UserMapper;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserRepository;
import org.example.user.repository.UserSettingsRepository;
import org.example.user.utils.UserLookupService;
import org.example.user.utils.UserUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — регистрация, профиль, приватность, репутация")
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSettingsRepository settingsRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private UserUpdateService userUpdateService;
    @Mock
    private IHttpCore httpCore;
    @Mock
    private org.example.user.client.FriendBlockClient friendBlockClient;
    @Mock
    private OutboxService outboxService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AppMetrics appMetrics;
    @Mock
    private EmailOtpService emailOtpService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setFriendUrl() {
        ReflectionTestUtils.setField(userService, "friendBaseUrl", "http://friend:8082");
        lenient().when(friendBlockClient.status(any(), any())).thenReturn(new BlockStatusDto(false, false));
        lenient().when(friendBlockClient.hiddenUserIds(any())).thenReturn(java.util.Set.of());
    }

    @Nested
    @DisplayName("Регистрация")
    class SignUp {

        @Test
        @DisplayName("Успех: пользователь сохраняется, пароль хешируется, событие уходит в outbox")
        void successWritesOutbox() {
            var request = UserFixtures.registration();
            UserEntity mapped = UserFixtures.user(0L);
            mapped.setId(null);
            UserEntity saved = UserFixtures.user(11L);
            UserDto dto = UserFixtures.publicDto(11L);

            when(userRepository.isExistByEmailOrNumberPhone(request.getEmail(), request.getNumberPhone()))
                    .thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(mapped);
            when(passwordEncoder.encode("Secret123")).thenReturn("$2a$new");
            when(userRepository.saveAndFlush(mapped)).thenReturn(saved);
            when(userMapper.toDto(saved)).thenReturn(dto);

            UserDto result = userService.signUp(request);

            assertEquals(11L, result.getUserId());
            assertEquals("$2a$new", mapped.getPassword());
            verify(outboxService).enqueue(eq(UserKafkaTopics.REGISTERED), eq(11L), any(UserRegisteredEvent.class));
            verify(appMetrics).registraciyaUspeh();
            verify(emailOtpService).sendVerifyCodeForNewUser(saved);
        }

        @Test
        @DisplayName("Без согласия с правилами — не регистрируем")
        void rejectsWithoutTerms() {
            var request = UserFixtures.registration();
            request.setAcceptedTerms(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.signUp(request));

            assertEquals(
                    "Чтобы создать аккаунт, примите Правила сообщества и Политику конфиденциальности.",
                    ex.getMessage());
            verify(userRepository, never()).saveAndFlush(any());
            verify(emailOtpService, never()).sendVerifyCodeForNewUser(any());
        }

        @Test
        @DisplayName("Email или телефон уже есть — IllegalStateException, в БД не пишем")
        void duplicatePrecheck() {
            var request = UserFixtures.registration();
            when(userRepository.isExistByEmailOrNumberPhone(request.getEmail(), request.getNumberPhone()))
                    .thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.signUp(request));

            assertEquals("Email или телефон уже используются!", ex.getMessage());
            verify(userRepository, never()).saveAndFlush(any());
            verify(appMetrics).registraciyaOshibka();
        }

        @Test
        @DisplayName("Гонка уникальности на save — тоже конфликт, без ложного успеха")
        void duplicateOnFlush() {
            var request = UserFixtures.registration();
            UserEntity mapped = UserFixtures.user(0L);
            mapped.setId(null);
            when(userRepository.isExistByEmailOrNumberPhone(any(), any())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(mapped);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.saveAndFlush(mapped)).thenThrow(new DataIntegrityViolationException("unique"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.signUp(request));

            assertEquals("Email или телефон уже используются!", ex.getMessage());
            verify(appMetrics).registraciyaOshibka();
            verify(outboxService, never()).enqueue(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Свой профиль и публичный профиль")
    class Profiles {

        @Test
        @DisplayName("getMyProfile отдаёт полный DTO и видимость фото из настроек")
        void myProfile() {
            UserEntity user = UserFixtures.user(1L);
            UserFixtures.settings(user, PhotoVisibility.FRIENDS);
            UserFullDto full = UserFixtures.fullDto(1L);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toFullDto(user)).thenReturn(full);

            UserFullDto result = userService.getMyProfile(1L);

            assertEquals("FRIENDS", result.getPhotoVisibility());
            assertTrue(result.isOnline());
        }

        @Test
        @DisplayName("Забаненный не получает свой профиль")
        void bannedCannotLoadOwnProfile() {
            UserEntity user = UserFixtures.user(1L);
            user.setAccountStatus(AccountStatus.BANNED);
            when(userLookupService.getById(1L)).thenReturn(user);

            assertThrows(org.example.user.exception.ForbiddenException.class, () -> userService.getMyProfile(1L));
        }

        @Test
        @DisplayName("Поиск по email: нет пользователя — EntityNotFoundException")
        void searchByEmailMissing() {
            when(userRepository.findByEmailIgnoreCase("no@mail.test")).thenReturn(java.util.Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> userService.searchUserByEmail("no@mail.test"));
        }
    }

    @Nested
    @DisplayName("Обновление аккаунта")
    class UpdateAccount {

        @Test
        @DisplayName("Чужой email уже занят — конфликт, outbox не пишется")
        void emailTakenByOther() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(userRepository.existsEmailForOtherUser("taken@example.com", 1L)).thenReturn(true);

            UpdateUserDto patch = new UpdateUserDto();
            patch.setEmail("taken@example.com");

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> userService.updateUserAccount(1L, patch)
            );
            assertEquals("Email уже используется!", ex.getMessage());
            verify(userUpdateService, never()).updateAccount(any(), any());
        }

        @Test
        @DisplayName("Смена email: после сохранения в outbox уходит EMAIL_UPDATED")
        void emailChangeEnqueued() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(userRepository.existsEmailForOtherUser("new@example.com", 1L)).thenReturn(false);
            UpdateUserDto patch = new UpdateUserDto();
            patch.setEmail("new@example.com");
            UserDto mapped = UserFixtures.publicDto(1L);
            when(userUpdateService.updateAccount(user, patch)).thenAnswer(inv -> {
                user.setEmail("new@example.com");
                return mapped;
            });

            userService.updateUserAccount(1L, patch);

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).enqueue(eq(UserKafkaTopics.EMAIL_UPDATED), eq(1L), payload.capture());
            assertTrue(payload.getValue() instanceof UserEmailUpdatedEvent);
        }

        @Test
        @DisplayName("Email не менялся — событие EMAIL_UPDATED не публикуем")
        void sameEmailNoEvent() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);
            UpdateUserDto patch = new UpdateUserDto();
            patch.setCity("Казань");
            when(userUpdateService.updateAccount(user, patch)).thenReturn(UserFixtures.publicDto(1L));

            userService.updateUserAccount(1L, patch);

            verify(outboxService, never()).enqueue(eq(UserKafkaTopics.EMAIL_UPDATED), any(), any());
        }
    }

    @Nested
    @DisplayName("Пароль, bio, репутация")
    class OtherUpdates {

        @Test
        @DisplayName("Смена пароля пишет PASSWORD_UPDATED в outbox")
        void passwordOutbox() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);
            UpdatePasswordUserDto dto = new UpdatePasswordUserDto("old", "NewPass123");

            userService.updatePassword(1L, dto);

            verify(userUpdateService).updatePassword(user, dto);
            verify(userRepository).save(user);
            verify(outboxService).enqueue(eq(UserKafkaTopics.PASSWORD_UPDATED), eq(1L), any(UserPasswordUpdatedEvent.class));
        }

        @Test
        @DisplayName("Обновление bio сохраняет текст как есть")
        void updateBio() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);

            userService.updateBio(1L, new UpdateBioDto("Новый статус"));

            assertEquals("Новый статус", user.getBio());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Репутация: 0 обновлённых строк — пользователь не найден")
        void reputationMissingUser() {
            when(userRepository.updateReputation(5L, 1L)).thenReturn(0);
            assertThrows(EntityNotFoundException.class, () -> userService.updateReputation(5L, 1L));
        }

        @Test
        @DisplayName("Репутация: есть обновлённая строка — без исключения")
        void reputationOk() {
            when(userRepository.updateReputation(5L, -1L)).thenReturn(1);
            userService.updateReputation(5L, -1L);
            verify(userRepository).updateReputation(5L, -1L);
        }
    }

    @Nested
    @DisplayName("Профиль для зрителя и фото")
    class ViewerProfile {

        @Test
        @DisplayName("Смотрит сам себя — сообщения, комментарии и фото разрешены")
        void selfViewer() {
            UserEntity user = UserFixtures.user(1L);
            UserFixtures.settings(user, PhotoVisibility.NONE);
            when(userLookupService.getById(1L)).thenReturn(user);
            when(userMapper.toPublicDto(user)).thenReturn(UserFixtures.publicDto(1L));

            UserDto dto = userService.getUserProfileForViewer(1L, 1L);

            assertTrue(dto.isCanMessage());
            assertTrue(dto.isCanComment());
            assertTrue(dto.isCanSeePhotos());
            assertEquals("NONE", dto.getPhotoVisibility());
        }

        @Test
        @DisplayName("Чужой профиль, фото только для друзей, дружбы нет — фото скрыты")
        void friendsOnlyWithoutFriendship() {
            UserEntity target = UserFixtures.user(2L);
            UserFixtures.settings(target, PhotoVisibility.FRIENDS);
            target.getSettings().setAllowDmFromAll(false);
            target.getSettings().setAllowCommentsFromAll(false);
            when(userLookupService.getById(2L)).thenReturn(target);
            when(userMapper.toPublicDto(target)).thenReturn(UserFixtures.publicDto(2L));
            when(httpCore.get(any(RequestData.class), eq(Boolean.class)))
                    .thenReturn(ResponseEntity.ok(false));

            UserDto dto = userService.getUserProfileForViewer(2L, 1L);

            assertFalse(dto.isCanSeePhotos());
            assertFalse(dto.isCanMessage());
            assertFalse(dto.isCanComment());
        }

        @Test
        @DisplayName("Дружба открывает ЛС, комментарии и фото при режиме FRIENDS")
        void friendsUnlocksAccess() {
            UserEntity target = UserFixtures.user(2L);
            UserFixtures.settings(target, PhotoVisibility.FRIENDS);
            target.getSettings().setAllowDmFromAll(false);
            target.getSettings().setAllowCommentsFromAll(false);
            when(userLookupService.getById(2L)).thenReturn(target);
            when(userMapper.toPublicDto(target)).thenReturn(UserFixtures.publicDto(2L));
            when(httpCore.get(any(RequestData.class), eq(Boolean.class)))
                    .thenReturn(ResponseEntity.ok(true));

            UserDto dto = userService.getUserProfileForViewer(2L, 1L);

            assertTrue(dto.isCanSeePhotos());
            assertTrue(dto.isCanMessage());
            assertTrue(dto.isCanComment());
        }

        @Test
        @DisplayName("Ошибка friend-сервиса не роняет профиль: считаем, что дружбы нет")
        void friendCheckFailureDefaultsToFalse() {
            UserEntity target = UserFixtures.user(2L);
            UserFixtures.settings(target, PhotoVisibility.FRIENDS);
            when(userLookupService.getById(2L)).thenReturn(target);
            when(userMapper.toPublicDto(target)).thenReturn(UserFixtures.publicDto(2L));
            when(httpCore.get(any(RequestData.class), eq(Boolean.class)))
                    .thenThrow(new RuntimeException("friend down"));

            UserDto dto = userService.getUserProfileForViewer(2L, 1L);

            assertFalse(dto.isCanSeePhotos());
        }
    }

    @Nested
    @DisplayName("Присутствие online / lastSeen")
    class Presence {

        @Test
        @DisplayName("Пустой список id — пустая карта, без обращения к БД")
        void emptyIds() {
            Map<Long, UserPresenceDto> result = userService.getPresence(List.of());
            assertTrue(result.isEmpty());
            verify(userRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Пользователь скрыл lastSeen — online=false и lastSeen=null даже если был в сети")
        void hiddenLastSeen() {
            UserEntity user = UserFixtures.user(1L);
            user.setLastSeenAt(LocalDateTime.now());
            UserFixtures.settings(user, PhotoVisibility.ALL).setShowLastSeen(false);
            when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user));

            Map<Long, UserPresenceDto> result = userService.getPresence(List.of(1L, 2L));

            assertFalse(result.get(1L).isOnline());
            assertNull(result.get(1L).getLastSeenAt());
            assertFalse(result.get(2L).isOnline());
        }
    }

    @Nested
    @DisplayName("Мягкое удаление, бан и разбан")
    class AccountStatusLifecycle {

        @Test
        @DisplayName("Удаление ставит DELETED и пишет статус в outbox")
        void softDelete() {
            UserEntity user = UserFixtures.user(1L);
            when(userLookupService.getById(1L)).thenReturn(user);

            userService.deleteMyAccount(1L);

            assertEquals(AccountStatus.DELETED, user.getAccountStatus());
            verify(outboxService).enqueue(eq(UserKafkaTopics.ACCOUNT_STATUS_CHANGED), eq(1L), any(UserAccountStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Бан и разбан обратимы")
        void banThenUnban() {
            UserEntity user = UserFixtures.user(2L);
            when(userLookupService.getById(2L)).thenReturn(user);

            userService.banUser(2L);
            assertEquals(AccountStatus.BANNED, user.getAccountStatus());

            userService.unbanUser(2L);
            assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
            assertNull(user.getBannedAt());
        }

        @Test
        @DisplayName("Администратора забанить нельзя")
        void cannotBanAdmin() {
            UserEntity user = UserFixtures.user(4L);
            user.setPlatformRole(org.example.user.entity.PlatformRole.ADMIN);
            when(userLookupService.getById(4L)).thenReturn(user);

            assertThrows(org.example.user.exception.ForbiddenException.class, () -> userService.banUser(4L));
        }

        @Test
        @DisplayName("Удалённый аккаунт нельзя разбанить")
        void cannotUnbanDeleted() {
            UserEntity user = UserFixtures.user(3L);
            user.setAccountStatus(AccountStatus.DELETED);
            when(userLookupService.getById(3L)).thenReturn(user);

            assertThrows(IllegalStateException.class, () -> userService.unbanUser(3L));
        }

        @Test
        @DisplayName("Чужой удалённый профиль — заглушка без ЛС")
        void deletedProfileStub() {
            UserEntity target = UserFixtures.user(2L);
            target.setAccountStatus(AccountStatus.DELETED);
            UserFixtures.settings(target, PhotoVisibility.ALL);
            when(userLookupService.getById(2L)).thenReturn(target);
            when(userMapper.toPublicDto(target)).thenReturn(UserFixtures.publicDto(2L));

            UserDto dto = userService.getUserProfileForViewer(2L, 1L);

            assertEquals("Аккаунт", dto.getFirstName());
            assertEquals("удалён", dto.getLastName());
            assertFalse(dto.isCanMessage());
            assertEquals("DELETED", dto.getAccountStatus());
        }

        @Test
        @DisplayName("Личный блок отключает сообщения")
        void blockedDisablesMessage() {
            UserEntity target = UserFixtures.user(2L);
            UserFixtures.settings(target, PhotoVisibility.ALL);
            when(userLookupService.getById(2L)).thenReturn(target);
            when(userMapper.toPublicDto(target)).thenReturn(UserFixtures.publicDto(2L));
            when(friendBlockClient.status(1L, 2L)).thenReturn(new BlockStatusDto(true, false));

            UserDto dto = userService.getUserProfileForViewer(2L, 1L);

            assertTrue(dto.isBlockedByMe());
            assertFalse(dto.isCanMessage());
        }
    }
}
