package org.example.user.exception;


import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DefaultErrorMessage> handleException(Exception ex, HttpServletRequest request) {
        log.error("[ERROR] Unhandled exception", ex);

        return getResponseEntity("Internal Server Error",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                null,
                "INTERNAL_SERVER_ERROR"
        );
    }

//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<DefaultErrorMessage> handleAccessDeniedException(
//            AccessDeniedException ex, HttpServletRequest request) {
//        log.error("[ERROR] AccessDeniedException", ex);
//        return getResponseEntity("Access Denied",
//                "У вас нет доступа к этому ресурсу.",
//                HttpStatus.FORBIDDEN.value(),
//                request.getRequestURI(),
//                null,
//                "ACCESS_DENIED");
//    }

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