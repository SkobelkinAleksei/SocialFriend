package com.example.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !Profanity.contains(value);
    }
}
