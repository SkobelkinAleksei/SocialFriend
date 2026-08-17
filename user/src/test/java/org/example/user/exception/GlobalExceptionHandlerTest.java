package org.example.user.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — коды ответов API user-сервиса")
public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = request("/api/v1/social/users/me");

    @Test
    @DisplayName("Не найден пользователь → 404 и текст из исключения")
    void notFound() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleEntityNotFoundException(new EntityNotFoundException("Такой пользователь не был найден."), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().errorCode());
        assertEquals("Такой пользователь не был найден.", response.getBody().detail());
    }

    @Test
    @DisplayName("Конфликт email/телефона → 409")
    void conflict() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleIllegalStateException(new IllegalStateException("Email уже используется!"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email уже используется!", response.getBody().detail());
    }

    @Test
    @DisplayName("Unique constraint из БД → 409 без внутреннего SQL")
    void dataIntegrity() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email или телефон уже используются!", response.getBody().detail());
        assertEquals("DUPLICATE_USER_DATA", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Нарушение ограничения жалобы → 409 без текста про email")
    void reportDataIntegrity() {
        HttpServletRequest reportRequest = request("/api/v1/social/reports");
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("check constraint"), reportRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Не удалось сохранить жалобу", response.getBody().detail());
        assertEquals("REPORT_CONFLICT", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Неверный старый пароль → 401")
    void badPassword() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Неверный старый пароль!"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Неверный старый пароль!", response.getBody().detail());
    }

    @Test
    @DisplayName("Прочий IllegalArgumentException → 400")
    void badArgument() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Некорректная видимость фотографий."), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Запрет доступа → 403")
    void forbidden() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleForbidden(new ForbiddenException("Нельзя"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Нельзя", response.getBody().detail());
    }

    @Test
    @DisplayName("Нечитаемое тело запроса → 400 без деталей парсера")
    void malformedJson() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleUnreadableMessage(new HttpMessageNotReadableException("json"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Некорректный формат запроса", response.getBody().detail());
        assertNull(response.getBody().fieldErrors());
    }

    @Test
    @DisplayName("Неожиданная ошибка → 500 без внутреннего message")
    void unexpected() {
        ResponseEntity<DefaultErrorMessage> response =
                handler.handleException(new RuntimeException("secret stack"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody().detail());
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
