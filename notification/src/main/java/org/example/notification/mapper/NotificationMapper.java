package org.example.notification.mapper;

import com.example.common.kafka.NotificationDto;
import com.example.common.kafka.NotificationGroupDto;
import org.example.notification.entity.NotificationEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationEntity toEntity(NotificationDto notificationDto);
    NotificationDto toDto(NotificationEntity notificationEntity);
    NotificationGroupDto.NotificationItemDetails toItemDetails(NotificationEntity entity);

    @Mapping(target = "items", ignore = true)
    @Mapping(target = "mergedIds", ignore = true)
    @Mapping(target = "count", ignore = true)
    NotificationGroupDto toGroupDto(NotificationEntity entity);
}