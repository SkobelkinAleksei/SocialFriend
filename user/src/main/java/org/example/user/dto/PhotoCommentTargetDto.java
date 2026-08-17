package org.example.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoCommentTargetDto {
    private Long ownerId;
    private Long targetId;
    private String kind;
    private boolean canView;
    private boolean canComment;
}
