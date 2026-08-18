package org.example.user.exception;


import org.example.user.exception.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DefaultErrorMessage> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<DefaultErrorMessage.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new DefaultErrorMessage.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        String detail = fieldErrors.isEmpty()
                ? "Ошибка валидации"
                : fieldErrors.get(0).message();

        return getResponseEntity(
                "Validation Failed",
                detail,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                fieldErrors,
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DefaultErrorMessage> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[ERROR] HttpMessageNotReadableException: {}", ex.getMessage());
        return getResponseEntity(
                "Bad Request",
                "Некорректный формат запроса",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                null,
                "MALFORMED_REQUEST"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DefaultErrorMessage> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("[ERROR] DataIntegrityViolationException", ex);
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        boolean reportPath = path.contains("/reports");
        return getResponseEntity(
                reportPath ? "Conflict" : "Conflict",
                reportPath ? "Не удалось сохранить жалобу" : "Email или телефон уже используются!",
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                null,
                reportPath ? "REPORT_CONFLICT" : "DUPLICATE_USER_DATA"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DefaultErrorMessage> handleException(Exception ex, HttpServletRequest request) {
        log.error("[ERROR] Unhandled exception", ex);

        return getResponseEntity("Internal Server Error",
                "Произошла внутренняя ошибка",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                null,
                "INTERNAL_SERVER_ERROR"
        );
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<DefaultErrorMessage> handleRateLimited(
            RateLimitedException ex, HttpServletRequest request) {
        return getResponseEntity(
                "Too Many Requests",
                ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                request.getRequestURI(),
                null,
                "RATE_LIMITED"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DefaultErrorMessage> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.error("[ОШИБКА] Некорректный аргумент", ex);

        if (ex.getMessage() != null && ex.getMessage().contains("пароль")) {
            return getResponseEntity(
                    "Неверный пароль",
                    ex.getMessage(),
                    HttpStatus.UNAUTHORIZED.value(),
                    request.getRequestURI(),
                    null,
                    "НЕВЕРНЫЙ_ПАРОЛЬ"
            );
        } else {
            return getResponseEntity(
                    "Ошибка",
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    request.getRequestURI(),
                    null,
                    "ОШИБКА"
            );
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<DefaultErrorMessage> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request
    ) {
        log.error("[ERROR] IllegalStateException ", ex);

        return getResponseEntity("Illegal State",
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                null,
                "ILLEGAL_STATE"
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<DefaultErrorMessage> handleForbidden(
            ForbiddenException ex, HttpServletRequest request
    ) {
        log.warn("[ERROR] ForbiddenException: {}", ex.getMessage());
        return getResponseEntity(
                "Forbidden",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                null,
                "FORBIDDEN"
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<DefaultErrorMessage> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request
    ) {
        log.error("[ERROR] EntityNotFoundException", ex);

        return getResponseEntity(
                "Entity Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI(),
                null,
                "ENTITY_NOT_FOUND"
        );
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<DefaultErrorMessage> handleNumberFormatException(
            NumberFormatException ex, HttpServletRequest request
    ) {
        log.error("[ERROR] NumberFormatException", ex);

        return getResponseEntity(
                "Invalid Number Format",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                null,
                "NUMBER_FORMAT_ERROR"
        );
    }

    private ResponseEntity<DefaultErrorMessage> getResponseEntity(
            String title,
            String detail,
            int status,
            String instance,
            List<DefaultErrorMessage.FieldError> fieldErrors,
            String errorCode
    ) {
        var defaultErrorMessage = DefaultErrorMessage.builder()
                .title(title)
                .detail(detail)
                .status(status)
                .timestamp(Instant.now())
                .instance(instance)
                .errorCode(errorCode)
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(defaultErrorMessage);
    }
}
