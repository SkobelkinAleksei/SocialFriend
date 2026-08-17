package com.example.common.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    String city;
    String districtName;
    Long reputation;
    String bio;
    String avatarUrl;
    String coverMode;
    String coverColor;
    String coverUrl;
    String photoVisibility;
    boolean canSeePhotos;
    boolean canMessage;
    boolean canComment;
    boolean blockedByMe;
    boolean blockedMe;
    String accountStatus;
    boolean online;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastSeenAt;
}