package org.example.comment.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.rmi.AccessException;
import java.time.LocalDateTime;

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