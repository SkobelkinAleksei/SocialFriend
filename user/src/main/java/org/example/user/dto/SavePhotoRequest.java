package org.example.user.dto;

import lombok.Data;

@Data
public class SavePhotoRequest {
    private String sourceUrl;
    private Long albumId;
    private String newAlbumTitle;
    private Long photoId;
}
