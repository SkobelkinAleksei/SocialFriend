package org.example.comment.mapper;

import org.example.comment.dto.CommentDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentEntity toEntity(NewCommentDto commentDto);
    CommentDto toDto(CommentEntity commentEntity);
}