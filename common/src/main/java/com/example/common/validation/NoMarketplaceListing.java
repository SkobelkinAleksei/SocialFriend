package com.example.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = NoMarketplaceListingValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface NoMarketplaceListing {
    String message() default "Сервис не для объявлений о купле-продаже. Встреча — это совместное дело, а не витрина.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
