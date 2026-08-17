package org.example.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.user.validation.AgeRange;
import org.example.user.validation.UserValidationRules;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateUserDto implements Serializable {

    @Size(min = 2, max = 30, message = "Имя должно быть от 2 до 30 символов.")
    @Pattern(regexp = UserValidationRules.NAME, message = UserValidationRules.NAME_MESSAGE)
    String firstName;

    @Size(min = 2, max = 30, message = "Фамилия должна быть от 2 до 30 символов.")
    @Pattern(regexp = UserValidationRules.NAME, message = UserValidationRules.NAME_MESSAGE)
    String lastName;

    @Email(message = "Введите корректный email.")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов.")
    String email;

    @Pattern(regexp = UserValidationRules.PHONE, message = UserValidationRules.PHONE_MESSAGE)
    String numberPhone;

    @AgeRange
    LocalDate birthday;

    @Size(max = 50, message = "Название города слишком длинное.")
    String city;

    @Size(max = 150, message = "Адрес не должен превышать 150 символов.")
    String streetAddress;

    @Size(max = 100, message = "Название района слишком длинное.")
    String districtName;

    @DecimalMin(value = "-90.0", message = "Некорректная широта.")
    @DecimalMax(value = "90.0", message = "Некорректная широта.")
    BigDecimal homeLatitude;

    @DecimalMin(value = "-180.0", message = "Некорректная долгота.")
    @DecimalMax(value = "180.0", message = "Некорректная долгота.")
    BigDecimal homeLongitude;

    public void setFirstName(String firstName) {
        this.firstName = blankToNull(firstName);
    }

    public void setLastName(String lastName) {
        this.lastName = blankToNull(lastName);
    }

    public void setEmail(String email) {
        String normalized = blankToNull(email);
        this.email = normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public void setNumberPhone(String numberPhone) {
        this.numberPhone = RegistrationUserDto.normalizeRuPhone(numberPhone);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
