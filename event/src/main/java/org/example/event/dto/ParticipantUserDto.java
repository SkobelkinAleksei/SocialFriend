package org.example.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantUserDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
}