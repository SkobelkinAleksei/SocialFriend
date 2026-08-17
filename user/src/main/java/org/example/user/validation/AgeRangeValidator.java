package org.example.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class AgeRangeValidator implements ConstraintValidator<AgeRange, LocalDate> {

    private int min;
    private int max;

    @Override
    public void initialize(AgeRange annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(LocalDate birthday, ConstraintValidatorContext context) {
        if (birthday == null) {
            return true;
        }
        int years = Period.between(birthday, LocalDate.now()).getYears();
        if (years < min) {
            replaceMessage(context, "Возраст не соответствует");
            return false;
        }
        if (years > max || birthday.isAfter(LocalDate.now())) {
            replaceMessage(context, "Укажите корректную дату рождения.");
            return false;
        }
        return true;
    }

    private static void replaceMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
