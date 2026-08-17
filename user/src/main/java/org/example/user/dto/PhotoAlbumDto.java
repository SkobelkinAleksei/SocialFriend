package org.example.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAlbumDto {
    private Long id;
    private String title;
    private String kind;
    private String coverMode;
    private String coverUrl;
    private long photoCount;
}
