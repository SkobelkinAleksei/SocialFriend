package org.example.security.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** Login = email (поле username сохранено для совместимости с фронтом). */
    @NotBlank(message = "Email обязателен")
    @JsonAlias("email")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
