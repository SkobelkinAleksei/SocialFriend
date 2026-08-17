package org.example.comment.dto;

import lombok.Data;

@Data
public class PhotoCommentTargetDto {
    private Long ownerId;
    private Long targetId;
    private String kind;
    private boolean canView;
    private boolean canComment;
}
