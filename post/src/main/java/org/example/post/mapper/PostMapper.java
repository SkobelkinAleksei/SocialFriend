package org.example.post.mapper;

import org.example.post.dto.NewPostDto;
import org.example.post.dto.PostDto;
import org.example.post.dto.PostFullDto;
import org.example.post.entity.PostEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostEntity toEntity(PostDto postDto);
    PostEntity toEntity(NewPostDto newPostDto);
    PostDto toDto(PostEntity postEntity);
    PostFullDto toFullDto(PostEntity postEntity);

}