package org.example.user.dto;

import lombok.Data;

@Data
public class UpdateCoverRequest {
    private String mode;
    private String color;
    private String coverUrl;
}
