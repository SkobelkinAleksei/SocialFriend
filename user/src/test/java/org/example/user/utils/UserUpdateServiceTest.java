package org.example.user.utils;

import com.example.common.dto.event.UserDto;
import org.example.user.UserFixtures;
import org.example.user.dto.UpdatePasswordUserDto;
import org.example.user.dto.UpdateUserDto;
import org.example.user.entity.UserEntity;
import org.example.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUpdateService — частичное обновление профиля и пароля")
public class UserUpdateServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUpdateService updateService;

    @Nested
    @DisplayName("Обновление анкеты")
    class Account {

        @Test
        @DisplayName("Если DTO = null, обновление невозможно")
        void nullDtoRejected() {
            UserEntity user = UserFixtures.user(1L);
            assertThrows(NullPointerException.class, () -> updateService.updateAccount(user, null));
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("Пустые строки не затирают имя и email")
        void blankFieldsIgnored() {
            UserEntity user = UserFixtures.user(1L);
            UpdateUserDto patch = new UpdateUserDto();
            patch.setFirstName("   ");
            patch.setEmail("  ");
            when(userMapper.toDto(user)).thenReturn(new UserDto());

            updateService.updateAccount(user, patch);

            assertEquals("Анна", user.getFirstName());
            assertEquals("anna@example.com", user.getEmail());
        }

        @Test
        @DisplayName("Если birthday не пришёл (null), существующая дата рождения сохраняется")
        void birthdayNotClearedWhenAbsent() {
            UserEntity user = UserFixtures.user(1L);
            LocalDate original = user.getBirthday();
            UpdateUserDto patch = new UpdateUserDto();
            patch.setCity("Казань");
            when(userMapper.toDto(user)).thenReturn(new UserDto());

            updateService.updateAccount(user, patch);

            assertEquals(original, user.getBirthday());
            assertEquals("Казань", user.getCity());
        }

        @Test
        @DisplayName("Если birthday передан, дата в сущности меняется")
        void birthdayUpdatedWhenPresent() {
            UserEntity user = UserFixtures.user(1L);
            UpdateUserDto patch = new UpdateUserDto();
            patch.setBirthday(LocalDate.of(2000, 1, 2));
            when(userMapper.toDto(user)).thenReturn(new UserDto());

            updateService.updateAccount(user, patch);

            assertEquals(LocalDate.of(2000, 1, 2), user.getBirthday());
        }

        @Test
        @DisplayName("Непустые поля профиля перезаписываются")
        void nonBlankFieldsApplied() {
            UserEntity user = UserFixtures.user(1L);
            UpdateUserDto patch = new UpdateUserDto();
            patch.setFirstName("Мария");
            patch.setEmail("maria@example.com");
            patch.setNumberPhone("+79005556677");
            when(userMapper.toDto(user)).thenAnswer(inv -> {
                UserDto dto = new UserDto();
                dto.setFirstName(user.getFirstName());
                dto.setEmail(user.getEmail());
                return dto;
            });

            UserDto result = updateService.updateAccount(user, patch);

            assertEquals("Мария", user.getFirstName());
            assertEquals("maria@example.com", user.getEmail());
            assertEquals("+79005556677", user.getNumberPhone());
            assertEquals("Мария", result.getFirstName());
        }
    }

    @Nested
    @DisplayName("Смена пароля")
    class Password {

        @Test
        @DisplayName("Неверный старый пароль — IllegalArgumentException, новый хеш не пишется")
        void wrongOldPassword() {
            UserEntity user = UserFixtures.user(1L);
            when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> updateService.updatePassword(user, new UpdatePasswordUserDto("wrong", "NewPass123"))
            );

            assertEquals("Неверный старый пароль!", ex.getMessage());
            assertEquals("$2a$hashed", user.getPassword());
        }

        @Test
        @DisplayName("Верный старый пароль — в сущность пишется хеш нового")
        void passwordRehashed() {
            UserEntity user = UserFixtures.user(1L);
            when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$new");

            updateService.updatePassword(user, new UpdatePasswordUserDto("old", "NewPass123"));

            assertEquals("$2a$new", user.getPassword());
            verify(passwordEncoder).encode("NewPass123");
        }
    }
}
