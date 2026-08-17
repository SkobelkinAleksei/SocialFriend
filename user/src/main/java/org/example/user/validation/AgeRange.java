package org.example.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = AgeRangeValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface AgeRange {
    String message() default "Возраст не соответствует";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 14;

    int max() default 100;
}
