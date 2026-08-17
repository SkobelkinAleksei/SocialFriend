package org.example.event.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = EventScheduleWindowValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface EventScheduleWindow {
    String message() default "Встречу можно назначить от 15 минут до 3 месяцев вперёд.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
