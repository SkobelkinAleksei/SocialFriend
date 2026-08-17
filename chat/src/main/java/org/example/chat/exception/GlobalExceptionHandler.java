package org.example.chat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(jakarta.persistence.EntityNotFoundException ex) {
        return createResponse(HttpStatus.NOT_FOUND, "Не найдено", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return createResponse(HttpStatus.FORBIDDEN, "Доступ запрещён", ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        return createResponse(HttpStatus.FORBIDDEN, "Доступ запрещён", ex.getMessage());
    }

    @ExceptionHandler(ChatPhotoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePhotoNotFound(ChatPhotoNotFoundException ex) {
        return createResponse(HttpStatus.NOT_FOUND, "Не найдено", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createResponse(HttpStatus.BAD_REQUEST, "Некорректный запрос", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage() == null || err.getDefaultMessage().isBlank()
                        ? "Проверьте данные голосования"
                        : err.getDefaultMessage())
                .findFirst()
                .orElse("Проверьте данные голосования");
        return createResponse(HttpStatus.BAD_REQUEST, "Некорректный запрос", details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String details = String.format("Параметр '%s' должен быть числом", ex.getName());
        return createResponse(HttpStatus.BAD_REQUEST, "Неверный формат запроса", details);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormat(NumberFormatException ex) {
        return createResponse(HttpStatus.BAD_REQUEST, "Неверный формат запроса", "Ожидалось число");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("[Chat] Необработанная ошибка", ex);
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Системная ошибка сервиса чатов",
                "Не удалось выполнить операцию. Попробуйте позже"
        );
    }

    private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String error, String details) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), status.value(), error, details),
                status
        );
    }
}

record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String details
) {}
