package org.example.user.dto;

import lombok.Data;

@Data
public class TogglePhotoLikeRequest {
    private String kind;
    private String targetKey;
    private Long ownerId;
    private Long postId;
}
