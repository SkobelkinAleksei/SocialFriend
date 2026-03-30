package org.example.user.mapper;

import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.UserDto;
import org.example.user.dto.UserFullDto;
import org.example.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timeStamp", ignore = true)
    UserEntity toEntity(RegistrationUserDto userDto);

    UserFullDto toFullDto(UserEntity userEntity);

    @Mapping(target = "userId", source = "id")
    UserDto toDto(UserEntity userEntity);
}