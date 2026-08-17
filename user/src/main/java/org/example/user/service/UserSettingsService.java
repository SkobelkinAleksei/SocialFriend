package org.example.user.service;

import com.example.common.dto.UserSettingsDto;

public interface UserSettingsService {
    // Получить настройки текущего пользователя
    UserSettingsDto getSettings(Long userId);

    // Сохранить измененные тумблеры и радиус из модалок фронтенда
    void updateSettings(Long userId, UserSettingsDto dto);
}
