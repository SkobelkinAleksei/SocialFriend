package org.example.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.config.JwtUtils;
import org.example.security.dto.ConsumedRefreshToken;
import org.example.security.dto.JwtResponse;
import org.example.security.dto.LoginRequest;
import org.example.security.dto.RefreshTokenRequest;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.service.RefreshTokenService;
import org.example.security.service.SecurityUserService;
import org.example.security.util.LoginNormalizer;
import com.example.common.metrics.AppMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/social/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final SecurityUserService securityUserService;
    private final AppMetrics appMetrics;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String login = LoginNormalizer.normalize(loginRequest.getUsername());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, loginRequest.getPassword())
            );
        } catch (BadCredentialsException ex) {
            securityUserService.registerFailedAttempt(login);
            appMetrics.vhodOshibka();
            log.warn("[Вход] Неверный логин или пароль: {}", login);
            throw ex;
        } catch (LockedException ex) {
            appMetrics.vhodOshibka();
            log.warn("[Вход] Попытка входа в заблокированный аккаунт: {}", login);
            throw ex;
        } catch (DisabledException ex) {
            appMetrics.vhodOshibka();
            log.warn("[Вход] Аккаунт отключён: {}", login);
            throw new DisabledException(disabledMessage(securityUserService.loadAccountStatusByUsername(login)));
        }

        SecurityUserDetails principal = (SecurityUserDetails) authentication.getPrincipal();
        securityUserService.resetFailedAttempts(principal.getId());

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.issueRefreshToken(principal.getId());
        appMetrics.vhodUspeh();
        log.info("[Вход] Успешный вход userId={}", principal.getId());
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        ConsumedRefreshToken consumed = refreshTokenService.validateAndConsume(request.getRefreshToken());
        var details = securityUserService.loadUserById(consumed.userId());
        if (!details.isEnabled()) {
            throw new DisabledException(disabledMessage(securityUserService.loadAccountStatus(consumed.userId())));
        }

        String accessToken = jwtUtils.generateAccessToken(consumed.userId(), securityUserService.loadPlatformRole(consumed.userId()));
        String newRefreshToken = refreshTokenService.issueRefreshToken(consumed.userId(), consumed.familyId());
        return ResponseEntity.ok(new JwtResponse(accessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    private static String disabledMessage(String status) {
        if ("BANNED".equalsIgnoreCase(status)) {
            return "Аккаунт заблокирован";
        }
        if ("DELETED".equalsIgnoreCase(status)) {
            return "Этот аккаунт удалён";
        }
        return "Вход в этот аккаунт недоступен";
    }
}
