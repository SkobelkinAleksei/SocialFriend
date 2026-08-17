package org.example.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("LoginNormalizer — канонический email для входа")
public class LoginNormalizerTest {

    @Test
    @DisplayName("null остаётся null")
    void nullSafe() {
        assertNull(LoginNormalizer.normalize(null));
    }

    @Test
    @DisplayName("Пробелы обрезаются, регистр нижний")
    void trimAndLower() {
        assertEquals("anna@example.com", LoginNormalizer.normalize("  Anna@Example.COM "));
    }
}
