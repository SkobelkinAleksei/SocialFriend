package org.example.post.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return createResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации", details);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                "Запрашиваемый пост не найден",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ForbiddenException ex) {
        return createResponse(
                HttpStatus.FORBIDDEN,
                "Отказ в доступе",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректное действие с постом",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        return createResponse(
                HttpStatus.CONFLICT,
                "Конфликт состояния",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Параметр '%s' имеет неверный формат", ex.getName());
        return createResponse(HttpStatus.BAD_REQUEST, "Ошибка формата данных", message);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormat(NumberFormatException ex) {
        return createResponse(HttpStatus.BAD_REQUEST, "Ошибка формата данных", "Некорректный числовой параметр");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("[CRITICAL ERROR]: ", ex);
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Системная ошибка сервиса постов",
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
