package org.example.comment.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.expression.AccessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                "Объект не найден",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AccessException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessException ex) {
        return createResponse(
                HttpStatus.FORBIDDEN,
                "Нарушение прав доступа",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return createResponse(
                HttpStatus.FORBIDDEN,
                "Отказ в доступе",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicError(IllegalStateException ex) {
        return createResponse(
                HttpStatus.CONFLICT,
                "Действие запрещено настройками автора",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detail = fieldErrors.isEmpty()
                ? "Ошибка валидации"
                : fieldErrors.get(0).getDefaultMessage();
        return createResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации", detail);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("[Comment] Конфликт целостности: {}", ex.getMostSpecificCause().getMessage());
        return createResponse(
                HttpStatus.CONFLICT,
                "Конфликт данных",
                "Это действие уже было выполнено"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("[CRITICAL ERROR] В сервисе комментариев произошел сбой: ", ex);
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервиса комментариев",
                "Попробуйте повторить операцию позже"
        );
    }

    private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String error, String details) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                details
        );
        return new ResponseEntity<>(response, status);
    }
}

record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String details
) {}
