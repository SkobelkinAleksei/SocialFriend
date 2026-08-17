package org.example.user.utils;

import jakarta.persistence.EntityNotFoundException;
import org.example.user.UserFixtures;
import org.example.user.entity.UserEntity;
import org.example.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupService — поиск пользователя по id")
public class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService lookupService;

    @Nested
    @DisplayName("Успешный поиск")
    class Success {

        @Test
        @DisplayName("Если пользователь есть в БД, возвращается та же сущность")
        void returnsEntityWhenFound() {
            UserEntity stored = UserFixtures.user(7L);
            when(userRepository.findById(7L)).thenReturn(Optional.of(stored));

            UserEntity result = lookupService.getById(7L);

            assertSame(stored, result);
            assertEquals(7L, result.getId());
        }
    }

    @Nested
    @DisplayName("Ошибки")
    class Errors {

        @Test
        @DisplayName("Если id = null, ожидаем NullPointerException и к БД не ходим")
        void nullIdRejected() {
            assertThrows(NullPointerException.class, () -> lookupService.getById(null));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Если пользователя нет, ожидаем EntityNotFoundException")
        void missingUser() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> lookupService.getById(99L)
            );
            assertEquals("Такой пользователь не был найден.", ex.getMessage());
        }
    }
}
