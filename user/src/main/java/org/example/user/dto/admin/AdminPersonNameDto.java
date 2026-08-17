package org.example.user.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminPersonNameDto {
    Long id;
    String firstName;
    String lastName;
    String email;
}
