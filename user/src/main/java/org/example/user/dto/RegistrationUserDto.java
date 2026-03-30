package org.example.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationUserDto implements Serializable {

    @NotBlank(message = "Имя должно быть указано.")
    @Size(min = 2, max = 20, message = "Имя должно быть от 2 до 20 символов.")
    String firstName;

    @NotBlank(message = "Фамилия должна быть указана.")
    @Size(min = 2, max = 20, message = "Фамилия должна быть от 2 до 20 символов.")
    String lastName;

    @Email
    @NotBlank(message = "Email не может быть пуст.")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов.")
    String email;

    @Pattern(regexp = "\\+7[0-9]{10}", message = "Телефонный номер должен начинаться с +7, затем - 10 цифр.")
    String numberPhone;

    @NotBlank(message = "Пароль не может быть пустым.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d!]{8,100}$",
            message = "Пароль должен содержать хотя бы одну заглавную букву," +
                    " хотя бы одну цифру, только английские символы и иметь длину от 8 до 100 символов."
    )
    String password;

    @NotNull(message = "Дата рождения должна быть указана.")
    @Past(message = "Дата рождения не может превышать текущую дату.")
    LocalDate birthday;
}
