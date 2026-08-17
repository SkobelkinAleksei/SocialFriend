package com.example.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoMarketplaceListingValidator implements ConstraintValidator<NoMarketplaceListing, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return !MarketplaceListing.isListing(value);
    }
}
