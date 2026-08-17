package com.example.common.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordUpdatedEvent {
    private Long userId;

    /** BCrypt hash from user-service. Alias keeps old "newPassword" payloads working. */
    @JsonProperty("passwordHash")
    @JsonAlias("newPassword")
    private String passwordHash;
}
