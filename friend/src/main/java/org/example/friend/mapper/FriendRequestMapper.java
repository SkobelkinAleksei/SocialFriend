package org.example.friend.mapper;

import org.example.friend.dto.FriendRequestDto;
import org.example.friend.entity.FriendRequestEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FriendRequestMapper {
    FriendRequestDto toDto(FriendRequestEntity friendRequestEntity);
}