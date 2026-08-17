package org.example.post.mapper;

import com.example.common.dto.PostDto;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.PostFullDto;
import org.example.post.entity.PostEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    @org.mapstruct.Mapping(target = "hiddenReason", ignore = true)
    PostEntity toEntity(PostDto postDto);
    @org.mapstruct.Mapping(target = "hiddenReason", ignore = true)
    PostEntity toEntity(NewPostDto newPostDto);
    @org.mapstruct.Mapping(target = "canComment", ignore = true)
    @org.mapstruct.Mapping(target = "likesCount", ignore = true)
    @org.mapstruct.Mapping(target = "liked", ignore = true)
    @org.mapstruct.Mapping(target = "commentsCount", ignore = true)
    PostDto toDto(PostEntity postEntity);
    PostFullDto toFullDto(PostEntity postEntity);

}