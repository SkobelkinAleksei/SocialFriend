package org.example.event.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class EventScheduleWindowValidator implements ConstraintValidator<EventScheduleWindow, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (value.isBefore(now.plusMinutes(15))) {
            replace(context, "Встречу можно назначить не раньше чем через 15 минут.");
            return false;
        }
        if (value.isAfter(now.plusMonths(3))) {
            replace(context, "Встречу можно назначить не позже чем через 3 месяца.");
            return false;
        }
        return true;
    }

    private static void replace(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
