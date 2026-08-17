package org.example.comment.mapper;

import org.example.comment.dto.CommentDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", ignore = true)
    @Mapping(target = "targetType", ignore = true)
    @Mapping(target = "targetId", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "commentStatus", ignore = true)
    CommentEntity toEntity(NewCommentDto commentDto);

    @Mapping(target = "authorFirstName", ignore = true)
    @Mapping(target = "authorLastName", ignore = true)
    @Mapping(target = "authorAvatarUrl", ignore = true)
    @Mapping(target = "replyToAuthorName", ignore = true)
    @Mapping(target = "likesCount", ignore = true)
    @Mapping(target = "dislikesCount", ignore = true)
    @Mapping(target = "myVote", ignore = true)
    CommentDto toDto(CommentEntity commentEntity);
}
