package org.example.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateUserDto implements Serializable {

    @Size(min = 2, max = 20, message = "Имя должно быть от 2 до 20 символов.")
    String firstName;

    @Size(min = 2, max = 20, message = "Фамилия должна быть от 2 до 20 символов.")
    String lastName;

    @Email
    @Size(max = 100, message = "Email не может быть длиннее 100 символов.")
    String email;

    @Pattern(regexp = "\\+7[0-9]{10}", message = "Телефонный номер должен начинаться с +7, затем - 10 цифр.")
    String numberPhone;

    @Past(message = "Дата рождения не может превышать текущую дату.")
    LocalDate birthday;
}
