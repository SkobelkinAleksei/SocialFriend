package org.example.user.exception;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record DefaultErrorMessage(
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        String errorCode,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}