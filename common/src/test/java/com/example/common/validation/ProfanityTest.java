package com.example.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Profanity — фильтр ненормативной лексики")
public class ProfanityTest {

    @Test
    @DisplayName("Пустая строка и null считаются чистыми")
    void blankIsClean() {
        assertFalse(Profanity.contains(null));
        assertFalse(Profanity.contains("   "));
        assertNull(Profanity.findMatch(""));
    }

    @Test
    @DisplayName("Обычный текст без мата проходит")
    void normalText() {
        assertFalse(Profanity.contains("Приглашаю соседей на прогулку в парке"));
    }

    @Test
    @DisplayName("Русский мат в любом регистре ловится")
    void russianStem() {
        assertTrue(Profanity.contains("Это просто БЛЯДЬ"));
        assertTrue(Profanity.contains("пиздец какой день"));
    }

    @Test
    @DisplayName("Английский мат ловится как отдельное слово, а не как часть другого")
    void englishWordBoundary() {
        assertTrue(Profanity.contains("what the fuck"));
        assertFalse(Profanity.contains("classic"));
    }
}
