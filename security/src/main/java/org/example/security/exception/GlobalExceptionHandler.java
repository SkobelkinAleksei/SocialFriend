package org.example.security.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
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
        String detail = fieldErrors.isEmpty() ? "Ошибка валидации" : fieldErrors.get(0).message();
        return build("Validation Failed", detail, HttpStatus.BAD_REQUEST, request.getRequestURI(),
                fieldErrors, "VALIDATION_ERROR");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<DefaultErrorMessage> handleLocked(
            LockedException ex, HttpServletRequest request) {
        return build("Too Many Requests", "Слишком много попыток входа. Попробуйте позже.",
                HttpStatus.TOO_MANY_REQUESTS, request.getRequestURI(), null, "ACCOUNT_LOCKED");
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<DefaultErrorMessage> handleDisabled(
            org.springframework.security.authentication.DisabledException ex, HttpServletRequest request) {
        String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Вход в этот аккаунт недоступен"
                : ex.getMessage();
        return build("Forbidden", detail, HttpStatus.UNAUTHORIZED, request.getRequestURI(),
                null, "ACCOUNT_DISABLED");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<DefaultErrorMessage> handleInvalidRefresh(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        return build("Unauthorized", "Недействительный refresh token", HttpStatus.UNAUTHORIZED,
                request.getRequestURI(), null, "INVALID_REFRESH");
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<DefaultErrorMessage> handleAuth(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("[AUTH] Ошибка аутентификации: {}", ex.getMessage());
        return build("Unauthorized", "Неверный email или пароль", HttpStatus.UNAUTHORIZED,
                request.getRequestURI(), null, "BAD_CREDENTIALS");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DefaultErrorMessage> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return build("Bad Request", ex.getMessage(), HttpStatus.BAD_REQUEST,
                request.getRequestURI(), null, "BAD_REQUEST");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<DefaultErrorMessage> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return build("Conflict", ex.getMessage(), HttpStatus.CONFLICT,
                request.getRequestURI(), null, "CONFLICT");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DefaultErrorMessage> handleException(
            Exception ex, HttpServletRequest request) {
        log.error("[ERROR] Unhandled exception", ex);
        return build("Internal Server Error", "Произошла внутренняя ошибка",
                HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI(), null, "INTERNAL_SERVER_ERROR");
    }

    private ResponseEntity<DefaultErrorMessage> build(
            String title,
            String detail,
            HttpStatus status,
            String instance,
            List<DefaultErrorMessage.FieldError> fieldErrors,
            String errorCode
    ) {
        return ResponseEntity.status(status).body(DefaultErrorMessage.builder()
                .title(title)
                .detail(detail)
                .status(status.value())
                .timestamp(Instant.now())
                .instance(instance)
                .errorCode(errorCode)
                .fieldErrors(fieldErrors)
                .build());
    }
}
