package org.example.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.example.user.validation.UserValidationRules;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordUserDto implements Serializable {

    @NotBlank(message = "Введите пароль для успешного обновления данных")
    String oldPassword;

    @Pattern(regexp = UserValidationRules.PASSWORD, message = UserValidationRules.PASSWORD_MESSAGE)
    String newPassword;
}