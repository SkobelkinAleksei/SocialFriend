package org.example.notification.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Ловим попытку изменить чужие уведомления и отдаем статус 403 Forbidden
    @ExceptionHandler(NotificationAccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(NotificationAccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 2. Ловим стандартную ошибку JPA, если уведомление не найдено в БД, и отдаем 404 Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Object> handleNumberFormat(NumberFormatException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Некорректный числовой параметр");
    }

    // 3. Фолбэк для всех остальных непредвиденных ошибок (статус 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка сервера");
    }

    // Вспомогательный метод для сборки одинаковой JSON-структуры ответов
    private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
        return new ResponseEntity<>(body, status);
    }
}
