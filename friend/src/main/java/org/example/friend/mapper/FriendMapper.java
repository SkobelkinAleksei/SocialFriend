package org.example.friend.mapper;

import org.example.friend.dto.FriendDto;
import org.example.friend.entity.FriendEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FriendMapper {

    @Mapping(target = "userId1", source = "userId1")
    @Mapping(target = "userId2", source = "userId2")
    FriendDto toDto(FriendEntity friendEntity);
}