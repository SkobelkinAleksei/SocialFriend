package org.example.notification.mapper;

import com.example.common.kafka.NotificationDto;
import org.example.notification.entity.NotificationEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationEntity toEntity(NotificationDto notificationDto);
    NotificationDto toDto(NotificationEntity notificationEntity);
}