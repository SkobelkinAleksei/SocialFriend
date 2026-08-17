package org.example.user.dto;

import lombok.Data;

@Data
public class UpdateAlbumRequest {
    private String title;
    private String coverMode;
    private Long coverPhotoId;
}
