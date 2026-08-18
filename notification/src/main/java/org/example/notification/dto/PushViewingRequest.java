package org.example.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushViewingRequest {
    @NotBlank
    private String endpoint;
    private boolean viewing;
}
