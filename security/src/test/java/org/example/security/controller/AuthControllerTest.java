package org.example.security.controller;

import com.example.common.metrics.AppMetrics;
import org.example.security.config.JwtUtils;
import org.example.security.dto.ConsumedRefreshToken;
import org.example.security.dto.JwtResponse;
import org.example.security.dto.LoginRequest;
import org.example.security.dto.RefreshTokenRequest;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.entity.UserSecurity;
import org.example.security.service.RefreshTokenService;
import org.example.security.service.SecurityUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — вход, refresh, выход")
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private SecurityUserService securityUserService;
    @Mock
    private AppMetrics appMetrics;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("Успешный вход: нормализуем email, сбрасываем неудачи, отдаём пару токенов")
    void loginOk() {
        LoginRequest request = new LoginRequest();
        request.setUsername("  Anna@Mail.RU ");
        request.setPassword("Secret123");

        UserSecurity user = new UserSecurity();
        user.setId(4L);
        user.setUsername("anna@mail.ru");
        user.setPassword("hash");
        user.setEnabled(true);
        Authentication auth = new UsernamePasswordAuthenticationToken(new SecurityUserDetails(user), null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateAccessToken(auth)).thenReturn("access");
        when(refreshTokenService.issueRefreshToken(4L)).thenReturn("refresh");

        ResponseEntity<JwtResponse> response = controller.authenticateUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("access", response.getBody().getToken());
        assertEquals("refresh", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        verify(securityUserService).resetFailedAttempts(4L);
        verify(appMetrics).vhodUspeh();
    }

    @Test
    @DisplayName("Неверный пароль — фиксируем попытку и пробрасываем BadCredentials")
    void loginBadPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("anna@mail.ru");
        request.setPassword("nope");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> controller.authenticateUser(request));
        verify(securityUserService).registerFailedAttempt("anna@mail.ru");
        verify(appMetrics).vhodOshibka();
        verify(refreshTokenService, never()).issueRefreshToken(any());
    }

    @Test
    @DisplayName("Заблокированный аккаунт — LockedException, счётчик не увеличиваем повторно")
    void loginLocked() {
        LoginRequest request = new LoginRequest();
        request.setUsername("anna@mail.ru");
        request.setPassword("x");
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThrows(LockedException.class, () -> controller.authenticateUser(request));
        verify(securityUserService, never()).registerFailedAttempt(any());
        verify(appMetrics).vhodOshibka();
    }

    @Test
    @DisplayName("Refresh: старый токен потребляется, выдаётся новая пара в той же семье")
    void refreshRotates() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old.refresh");
        when(refreshTokenService.validateAndConsume("old.refresh"))
                .thenReturn(new ConsumedRefreshToken(4L, "fam"));
        UserSecurity user = new UserSecurity();
        user.setId(4L);
        user.setUsername("anna@mail.ru");
        user.setPassword("hash");
        user.setEnabled(true);
        when(securityUserService.loadUserById(4L)).thenReturn(new SecurityUserDetails(user));
        when(securityUserService.loadPlatformRole(4L)).thenReturn("USER");
        when(jwtUtils.generateAccessToken(4L, "USER")).thenReturn("new-access");
        when(refreshTokenService.issueRefreshToken(4L, "fam")).thenReturn("new-refresh");

        ResponseEntity<JwtResponse> response = controller.refresh(request);

        assertEquals("new-access", response.getBody().getToken());
        assertEquals("new-refresh", response.getBody().getRefreshToken());
        verify(securityUserService).loadUserById(4L);
    }

    @Test
    @DisplayName("Logout отзывает refresh и отвечает 204")
    void logout() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("r");

        ResponseEntity<Void> response = controller.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(refreshTokenService).revoke("r");
    }
}
