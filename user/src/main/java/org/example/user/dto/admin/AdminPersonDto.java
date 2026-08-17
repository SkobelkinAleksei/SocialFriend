package org.example.user.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminPersonDto {
    Long id;
    String firstName;
    String lastName;
    String email;
    String accountStatus;
    String platformRole;
    long reportsTotal;
    long reportsUpheld;
}
