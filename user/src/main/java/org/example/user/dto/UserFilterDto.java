package org.example.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDto {
    private String firstName;
    private String lastName;
    private String numberPhone;
    private LocalDate timeStamp;
    private LocalDate birthdayFrom;
    private LocalDate birthdayTo;
}