package org.example.user.controller;

import com.example.common.dto.event.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.user.dto.EmailCodeRequest;
import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.ResetPasswordRequest;
import org.example.user.dto.VerifyEmailRequest;
import org.example.user.service.EmailOtpService;
import org.example.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/registration")
@RestController
public class PublicUserController {
    private final UserService userService;
    private final EmailOtpService emailOtpService;

    @PostMapping("/signUp")
    public ResponseEntity<UserDto> signUp(
            @Valid @RequestBody RegistrationUserDto registrationUserDto
    ) {
        return ResponseEntity.ok().body(userService.signUp(registrationUserDto));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailOtpService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody EmailCodeRequest request) {
        emailOtpService.sendVerifyCode(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailCodeRequest request) {
        emailOtpService.sendResetCode(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        emailOtpService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
