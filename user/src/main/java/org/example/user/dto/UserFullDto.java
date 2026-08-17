package org.example.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserFullDto implements Serializable {
    Long id;
    String firstName;
    String lastName;
    String email;
    String numberPhone;
    LocalDate birthday;
    String city;
    String streetAddress;
    String districtName;
    Long reputation;
    BigDecimal homeLatitude;
    BigDecimal homeLongitude;
    LocalDateTime timeStamp;
    String bio;
    String avatarUrl;
    String coverMode;
    String coverColor;
    String coverUrl;
    String photoVisibility;
    String accountStatus;
    String platformRole;
    boolean online;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastSeenAt;
}
