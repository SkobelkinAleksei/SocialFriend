package org.example.user.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements Serializable {
    Long userId;
    String firstName;
    String lastName;
    String numberPhone;
    String email;
    LocalDate birthday;
}