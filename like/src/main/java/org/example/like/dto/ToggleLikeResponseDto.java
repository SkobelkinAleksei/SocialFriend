package org.example.like.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToggleLikeResponseDto {
    private boolean liked;
    private long likesCount;
}
