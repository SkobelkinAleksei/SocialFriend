package org.example.scheduler.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createResponse(HttpStatus.BAD_REQUEST, "Некорректный запрос", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage() == null || err.getDefaultMessage().isBlank()
                        ? "Проверьте данные планировщика"
                        : err.getDefaultMessage())
                .findFirst()
                .orElse("Проверьте данные планировщика");
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        log.warn("[Scheduler] Конфликт целостности: {}", ex.getMostSpecificCause().getMessage());
        return createResponse(HttpStatus.CONFLICT, "Конфликт данных", "Задача уже существует");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("[Scheduler] Необработанная ошибка", ex);
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Системная ошибка планировщика",
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
