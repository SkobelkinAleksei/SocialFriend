package com.example.common.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent {
    private Long id;

    /** Login identifier (email). JSON alias keeps old "username" payloads working. */
    @JsonProperty("email")
    @JsonAlias("username")
    private String email;

    private String passwordHash;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** USER или ADMIN. Старые события без поля security читает как USER. */
    private String platformRole;

    /**
     * null в старых событиях = почта уже считалась подтверждённой.
     * Новая регистрация с района шлёт false.
     */
    private Boolean emailVerified;

    public UserRegisteredEvent(Long id, String email, String passwordHash, LocalDateTime createdAt) {
        this(id, email, passwordHash, createdAt, "USER", null);
    }

    public UserRegisteredEvent(Long id, String email, String passwordHash, LocalDateTime createdAt, String platformRole) {
        this(id, email, passwordHash, createdAt, platformRole, null);
    }

    public boolean emailVerifiedOrLegacy() {
        return emailVerified == null || Boolean.TRUE.equals(emailVerified);
    }
}
