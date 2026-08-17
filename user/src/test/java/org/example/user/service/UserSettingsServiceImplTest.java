package org.example.user.service;

import com.example.common.dto.UserSettingsDto;
import jakarta.persistence.EntityNotFoundException;
import org.example.user.UserFixtures;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.UserEntity;
import org.example.user.entity.UserSettingsEntity;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserSettingsRepository;
import org.example.user.utils.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingsServiceImpl — чтение и обновление настроек")
public class UserSettingsServiceImplTest {

    @Mock
    private UserSettingsRepository settingsRepository;
    @Mock
    private UserLookupService lookupService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UserSettingsServiceImpl settingsService;

    @Nested
    @DisplayName("Чтение")
    class Read {

        @Test
        @DisplayName("Настройки есть — возвращаем их, пользователя заново не создаём")
        void existingSettings() {
            UserEntity user = UserFixtures.user(3L);
            UserSettingsEntity entity = UserFixtures.settings(user, PhotoVisibility.FRIENDS);
            entity.setAllowDmFromAll(false);
            when(settingsRepository.findById(3L)).thenReturn(Optional.of(entity));

            UserSettingsDto dto = settingsService.getSettings(3L);

            assertEquals("FRIENDS", dto.getPhotoVisibility());
            assertFalse(dto.getAllowDmFromAll());
            verify(lookupService, never()).getById(any());
        }

        @Test
        @DisplayName("Настроек нет — создаём дефолтные ALL / все уведомления включены")
        void createDefaults() {
            UserEntity user = UserFixtures.user(4L);
            when(settingsRepository.findById(4L)).thenReturn(Optional.empty());
            when(lookupService.getById(4L)).thenReturn(user);
            when(settingsRepository.save(any(UserSettingsEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            UserSettingsDto dto = settingsService.getSettings(4L);

            assertEquals("ALL", dto.getPhotoVisibility());
            assertTrue(dto.getNotifyComments());
            verify(settingsRepository).save(any(UserSettingsEntity.class));
        }
    }

    @Nested
    @DisplayName("Обновление")
    class Update {

        @Test
        @DisplayName("Нет настроек — EntityNotFoundException, outbox пустой")
        void missing() {
            when(settingsRepository.findById(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> settingsService.updateSettings(9L, UserSettingsDto.builder().build()));
            verify(outboxService, never()).enqueue(any(), any(), any());
        }

        @Test
        @DisplayName("Неизвестная видимость фото — IllegalArgumentException")
        void badPhotoVisibility() {
            UserEntity user = UserFixtures.user(1L);
            UserSettingsEntity entity = UserFixtures.settings(user, PhotoVisibility.ALL);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(entity));

            UserSettingsDto patch = UserSettingsDto.builder().photoVisibility("SECRET").build();

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> settingsService.updateSettings(1L, patch)
            );
            assertEquals("Некорректная видимость фотографий.", ex.getMessage());
        }

        @Test
        @DisplayName("Частичный патч: меняем только DM, остальное не трогаем; событие в outbox")
        void partialPatchEnqueued() {
            UserEntity user = UserFixtures.user(1L);
            UserSettingsEntity entity = UserFixtures.settings(user, PhotoVisibility.FRIENDS);
            entity.setNotifyComments(true);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(settingsRepository.save(entity)).thenReturn(entity);

            UserSettingsDto patch = UserSettingsDto.builder()
                    .allowDmFromAll(false)
                    .build();

            settingsService.updateSettings(1L, patch);

            assertFalse(entity.getAllowDmFromAll());
            assertEquals(PhotoVisibility.FRIENDS, entity.getPhotoVisibility());
            assertTrue(entity.getNotifyComments());
            verify(outboxService).enqueue(eq(UserKafkaTopics.SETTINGS_UPDATED), eq(1L), any());
        }
    }
}
