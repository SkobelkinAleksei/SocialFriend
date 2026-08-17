package org.example.user.service;

import com.example.common.dto.UserSettingsDto;
import com.example.common.kafka.UserSettingsUpdatedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.UserEntity;
import org.example.user.entity.UserSettingsEntity;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserSettingsRepository;
import org.example.user.utils.UserLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository settingsRepository;
    private final UserLookupService lookupService;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public UserSettingsDto getSettings(Long userId) {
        log.info("[UserSettingsService] Запрос настроек для пользователя с id: {}", userId);

        UserSettingsEntity entity = settingsRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("[UserSettingsService] Настройки не найдены. Генерируем дефолтный профиль настроек для userId: {}", userId);
                    UserEntity userEntity = lookupService.getById(userId);

                    UserSettingsEntity defaultSettings = UserSettingsEntity.builder()
                            .userId(userEntity.getId())
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

                    return settingsRepository.save(defaultSettings);
                });

        return toDto(entity);
    }

    @Override
    @Transactional
    public void updateSettings(Long userId, UserSettingsDto dto) {
        log.info("[UserSettingsService] Обновление настроек для пользователя с id: {}", userId);
        UserSettingsEntity entity = settingsRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Настройки профиля не найдены для userId: " + userId));

        if (dto.getAllowDmFromAll() != null) entity.setAllowDmFromAll(dto.getAllowDmFromAll());
        if (dto.getAllowCommentsFromAll() != null) entity.setAllowCommentsFromAll(dto.getAllowCommentsFromAll());
        if (dto.getPhotoVisibility() != null && !dto.getPhotoVisibility().isBlank()) {
            try {
                entity.setPhotoVisibility(PhotoVisibility.valueOf(dto.getPhotoVisibility().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Некорректная видимость фотографий.");
            }
        }
        if (dto.getNotifyComments() != null) entity.setNotifyComments(dto.getNotifyComments());
        if (dto.getNotifyMessages() != null) entity.setNotifyMessages(dto.getNotifyMessages());
        if (dto.getNotifyEventRequests() != null) entity.setNotifyEventRequests(dto.getNotifyEventRequests());
        if (dto.getNotifyReputation() != null) entity.setNotifyReputation(dto.getNotifyReputation());
        if (dto.getShowLastSeen() != null) entity.setShowLastSeen(dto.getShowLastSeen());

        UserSettingsEntity savedEntity = settingsRepository.save(entity);
        log.info("[UserSettingsService] Настройки для пользователя {} успешно сохранены", userId);

        UserSettingsDto settingsDto = toDto(savedEntity);
        UserSettingsUpdatedEvent event = UserSettingsUpdatedEvent.builder()
                .userId(userId)
                .settings(settingsDto)
                .build();

        outboxService.enqueue(UserKafkaTopics.SETTINGS_UPDATED, userId, event);
        log.info("[Outbox] Настройки userId={} записаны в outbox", userId);
    }

    private static UserSettingsDto toDto(UserSettingsEntity entity) {
        return UserSettingsDto.builder()
                .allowDmFromAll(entity.getAllowDmFromAll())
                .allowCommentsFromAll(entity.getAllowCommentsFromAll())
                .photoVisibility(entity.getPhotoVisibility() == null ? "ALL" : entity.getPhotoVisibility().name())
                .notifyComments(entity.getNotifyComments())
                .notifyMessages(entity.getNotifyMessages())
                .notifyEventRequests(entity.getNotifyEventRequests())
                .notifyReputation(entity.getNotifyReputation())
                .showLastSeen(entity.getShowLastSeen() == null || entity.getShowLastSeen())
                .build();
    }
}
