package org.example.friend.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                "Объект не найден в базе данных",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicError(IllegalStateException ex) {
        return createResponse(
                HttpStatus.CONFLICT,
                "Действие невозможно из-за текущего состояния данных",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException ex) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Ошибка в переданных параметрах",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(AccessDeniedException ex) {
        return createResponse(
                HttpStatus.FORBIDDEN,
                "Нарушение прав доступа",
                ex.getMessage()
        );
    }

    // Ошибка, если вместо числа в URL передали строку или неверный Enum
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Параметр '%s' имеет неверный формат или тип", ex.getName());
        return createResponse(HttpStatus.BAD_REQUEST, "Ошибка приведения типов", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Критическая ошибка: ", ex);
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Произошла системная ошибка на сервере",
                "Попробуйте позже или обратитесь в поддержку"
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
