package org.example.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordUserDto implements Serializable {

    @NotBlank(message = "Введите пароль для успешного обновления данных")
    String oldPassword;

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "Пароль должен содержать хотя бы одну заглавную букву," +
                    " хотя бы одну цифру, только английские символы и иметь длину от 8 до 20 символов."
    )
    String newPassword;
}