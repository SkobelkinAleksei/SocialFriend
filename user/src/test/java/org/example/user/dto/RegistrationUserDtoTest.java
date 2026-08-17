package org.example.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("RegistrationUserDto — нормализация телефона и email")
public class RegistrationUserDtoTest {

    @Test
    @DisplayName("Номер с 8 и пробелами становится +7XXXXXXXXXX")
    void eightPrefixNormalized() {
        RegistrationUserDto dto = new RegistrationUserDto();
        dto.setNumberPhone("8 (900) 111-22-33");
        assertEquals("+79001112233", dto.getNumberPhone());
    }

    @Test
    @DisplayName("Номер уже в формате +7 остаётся каноническим")
    void plusSevenKept() {
        RegistrationUserDto dto = new RegistrationUserDto();
        dto.setNumberPhone("+79001112233");
        assertEquals("+79001112233", dto.getNumberPhone());
    }

    @Test
    @DisplayName("Email приводится к нижнему регистру и обрезается")
    void emailLowercased() {
        RegistrationUserDto dto = new RegistrationUserDto();
        dto.setEmail("  Anna@Example.COM ");
        assertEquals("anna@example.com", dto.getEmail());
    }

    @Test
    @DisplayName("Имя из одних пробелов становится null — сработает @NotBlank")
    void blankNameBecomesNull() {
        RegistrationUserDto dto = new RegistrationUserDto();
        dto.setFirstName("   ");
        assertNull(dto.getFirstName());
    }

    @Test
    @DisplayName("Пустой телефон → null")
    void emptyPhone() {
        assertNull(RegistrationUserDto.normalizeRuPhone("   "));
        assertNull(RegistrationUserDto.normalizeRuPhone(null));
    }
}
