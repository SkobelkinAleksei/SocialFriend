package com.example.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPersonPreviewDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
}
