package org.example.user.validation;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AgeRangeValidator — возраст при регистрации и правке профиля")
public class AgeRangeValidatorTest {

    private AgeRangeValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new AgeRangeValidator();
        validator.initialize(new AgeRange() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return AgeRange.class;
            }

            @Override
            public String message() {
                return "Возраст не соответствует";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int min() {
                return 14;
            }

            @Override
            public int max() {
                return 100;
            }
        });
        context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("null пропускаем: обязательность проверяет @NotNull, а не возраст")
    void nullIsValid() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("14 лет — нижняя граница, допустимо")
    void minAgeAllowed() {
        assertTrue(validator.isValid(LocalDate.now().minusYears(14), context));
    }

    @Test
    @DisplayName("13 лет — слишком молод")
    void tooYoung() {
        assertFalse(validator.isValid(LocalDate.now().minusYears(13), context));
    }

    @Test
    @DisplayName("101 год — слишком стар")
    void tooOld() {
        assertFalse(validator.isValid(LocalDate.now().minusYears(101), context));
    }

    @Test
    @DisplayName("Дата в будущем недопустима")
    void futureRejected() {
        assertFalse(validator.isValid(LocalDate.now().plusDays(1), context));
    }
}
