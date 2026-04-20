package org.example.like.mapper;

import org.example.like.dto.LikePostDto;
import org.example.like.entity.LikePostEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LikePostMapper {
    LikePostDto toDto(LikePostEntity likePostEntity);
}
