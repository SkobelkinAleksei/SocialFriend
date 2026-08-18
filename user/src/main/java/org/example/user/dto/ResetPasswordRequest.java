package org.example.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.example.user.validation.UserValidationRules;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Введите 6 цифр из письма")
    private String code;

    @NotBlank
    @Pattern(regexp = UserValidationRules.PASSWORD, message = UserValidationRules.PASSWORD_MESSAGE)
    private String newPassword;
}
