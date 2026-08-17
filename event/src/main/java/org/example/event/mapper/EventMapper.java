package org.example.event.mapper;

import com.example.common.dto.event.EventDto;
import org.example.event.dto.CreateEventDto;
import org.example.event.entity.enums.EventCategory;
import org.example.event.entity.EventEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryToString")
    @Mapping(target = "isPrivate", source = "private")
    @Mapping(target = "isPast", ignore = true)
    @Mapping(target = "canVoteReputation", ignore = true)
    @Mapping(target = "reputationOpensAt", ignore = true)
    @Mapping(target = "reputationClosesAt", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "organizerName", ignore = true)
    @Mapping(target = "organizerAvatarUrl", ignore = true)
    @Mapping(target = "peoplePreview", ignore = true)
    EventDto toDto(EventEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizerId", ignore = true)
    @Mapping(target = "currentParticipants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    EventEntity toEntity(CreateEventDto dto);

    // Enum перевести в строку для фронта
    @Named("categoryToString")
    default String categoryToString(EventCategory category) {
        if (category == null) {
            return null;
        }
        return category.name(); // Вернет "SOS", "Движ" или "События"
    }
}