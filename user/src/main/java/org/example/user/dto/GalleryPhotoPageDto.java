package org.example.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPhotoPageDto {
    @Builder.Default
    private List<GalleryPhotoDto> items = new ArrayList<>();
    private int page;
    private int size;
    private long total;
    private boolean hasMore;
    private boolean hidden;
}
