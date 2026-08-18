package org.example.user.mapper;

import com.example.common.dto.event.UserDto;
import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.UserFullDto;
import org.example.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timeStamp", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "coverMode", ignore = true)
    @Mapping(target = "coverColor", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "bannedAt", ignore = true)
    @Mapping(target = "anonymizedAt", ignore = true)
    @Mapping(target = "termsAcceptedAt", ignore = true)
    @Mapping(target = "termsVersion", ignore = true)
    @Mapping(target = "platformRole", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "reputation", ignore = true)
    @Mapping(target = "settings", ignore = true)
    UserEntity toEntity(RegistrationUserDto userDto);

    @Mapping(target = "photoVisibility", ignore = true)
    @Mapping(target = "online", ignore = true)
    @Mapping(target = "accountStatus", expression = "java(userEntity.effectiveAccountStatus().name())")
    @Mapping(target = "platformRole", expression = "java(userEntity.effectivePlatformRole().name())")
    UserFullDto toFullDto(UserEntity userEntity);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "photoVisibility", ignore = true)
    @Mapping(target = "canSeePhotos", ignore = true)
    @Mapping(target = "canMessage", ignore = true)
    @Mapping(target = "canComment", ignore = true)
    @Mapping(target = "blockedByMe", ignore = true)
    @Mapping(target = "blockedMe", ignore = true)
    @Mapping(target = "online", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @Mapping(target = "accountStatus", expression = "java(userEntity.effectiveAccountStatus().name())")
    UserDto toDto(UserEntity userEntity);

    /** Публичная карточка: без email и телефона. */
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "numberPhone", ignore = true)
    @Mapping(target = "photoVisibility", ignore = true)
    @Mapping(target = "canSeePhotos", ignore = true)
    @Mapping(target = "canMessage", ignore = true)
    @Mapping(target = "canComment", ignore = true)
    @Mapping(target = "blockedByMe", ignore = true)
    @Mapping(target = "blockedMe", ignore = true)
    @Mapping(target = "online", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @Mapping(target = "accountStatus", expression = "java(userEntity.effectiveAccountStatus().name())")
    UserDto toPublicDto(UserEntity userEntity);
}