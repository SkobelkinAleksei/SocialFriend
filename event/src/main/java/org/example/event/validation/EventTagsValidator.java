package org.example.event.validation;

import com.example.common.validation.MarketplaceListing;
import com.example.common.validation.Profanity;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class EventTagsValidator implements ConstraintValidator<EventTags, List<String>> {

    private static final int MAX_TAGS = 8;
    private static final int MIN_LEN = 2;
    private static final int MAX_LEN = 24;

    @Override
    public boolean isValid(List<String> tags, ConstraintValidatorContext context) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        if (tags.size() > MAX_TAGS) {
            replace(context, "Можно указать не больше 8 тегов.");
            return false;
        }
        for (String raw : tags) {
            if (raw == null || raw.isBlank()) {
                replace(context, "Тег не может быть пустым.");
                return false;
            }
            String tag = raw.trim();
            if (tag.length() < MIN_LEN || tag.length() > MAX_LEN) {
                replace(context, "Каждый тег — от 2 до 24 символов.");
                return false;
            }
            if (Profanity.contains(tag)) {
                replace(context, "В теге нельзя использовать ненормативную лексику.");
                return false;
            }
            if (MarketplaceListing.isListing(tag)) {
                replace(context, MarketplaceListing.MESSAGE);
                return false;
            }
        }
        return true;
    }

    private static void replace(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
