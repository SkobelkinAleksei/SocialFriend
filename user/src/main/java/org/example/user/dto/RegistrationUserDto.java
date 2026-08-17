package org.example.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.example.user.validation.AgeRange;
import org.example.user.validation.UserValidationRules;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;
import java.time.LocalDate;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationUserDto implements Serializable {

    @NotBlank(message = "Имя должно быть указано.")
    @Size(min = 2, max = 30, message = "Имя должно быть от 2 до 30 символов.")
    @Pattern(regexp = UserValidationRules.NAME, message = UserValidationRules.NAME_MESSAGE)
    String firstName;

    @NotBlank(message = "Фамилия должна быть указана.")
    @Size(min = 2, max = 30, message = "Фамилия должна быть от 2 до 30 символов.")
    @Pattern(regexp = UserValidationRules.NAME, message = UserValidationRules.NAME_MESSAGE)
    String lastName;

    @NotBlank(message = "Email не может быть пуст.")
    @Email(message = "Введите корректный email.")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов.")
    String email;

    @NotBlank(message = "Укажите номер телефона.")
    @Pattern(regexp = UserValidationRules.PHONE, message = UserValidationRules.PHONE_MESSAGE)
    String numberPhone;

    @NotBlank(message = "Пароль не может быть пустым.")
    @Pattern(regexp = UserValidationRules.PASSWORD, message = UserValidationRules.PASSWORD_MESSAGE)
    String password;

    @NotNull(message = "Дата рождения должна быть указана.")
    @AgeRange
    LocalDate birthday;

    @NotBlank(message = "Город должен быть указан.")
    @Size(max = 50, message = "Название города слишком длинное.")
    String city;

    @NotBlank(message = "Адрес проживания должен быть указан.")
    @Size(max = 150, message = "Адрес не должен превышать 150 символов.")
    String streetAddress;

    @NotBlank(message = "Район проживания должен быть указан.")
    @Size(max = 100, message = "Название района слишком длинное.")
    String districtName;

    @NotNull(message = "Широта домашней локации должна быть указана.")
    @DecimalMin(value = "-90.0", message = "Некорректная широта.")
    @DecimalMax(value = "90.0", message = "Некорректная широта.")
    BigDecimal homeLatitude;

    @NotNull(message = "Долгота домашней локации должна быть указана.")
    @DecimalMin(value = "-180.0", message = "Некорректная долгота.")
    @DecimalMax(value = "180.0", message = "Некорректная долгота.")
    BigDecimal homeLongitude;

    @NotNull(message = "Чтобы создать аккаунт, примите Правила сообщества и Политику конфиденциальности.")
    @AssertTrue(message = "Чтобы создать аккаунт, примите Правила сообщества и Политику конфиденциальности.")
    Boolean acceptedTerms;

    public void setFirstName(String firstName) {
        this.firstName = trimToNull(firstName);
    }

    public void setLastName(String lastName) {
        this.lastName = trimToNull(lastName);
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void setNumberPhone(String numberPhone) {
        this.numberPhone = normalizeRuPhone(numberPhone);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeRuPhone(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("8") && digits.length() == 11) {
            digits = "7" + digits.substring(1);
        }
        if (digits.startsWith("7") && digits.length() == 11) {
            return "+" + digits;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
