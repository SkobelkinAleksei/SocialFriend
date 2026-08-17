package org.example.event.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = EventTagsValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface EventTags {
    String message() default "Не больше 8 тегов, каждый от 2 до 24 символов.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
