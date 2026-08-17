package com.example.common.kafka;

import com.example.common.dto.UserSettingsDto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsUpdatedEvent {
    private Long userId;
    private UserSettingsDto settings;
}
