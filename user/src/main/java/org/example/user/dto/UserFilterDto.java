package org.example.user.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDto {
    private String firstName;
    private String lastName;
    private String numberPhone;
    private LocalDate timeStamp;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate birthdayFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate birthdayTo;

    private String city;
    private String districtName;
}