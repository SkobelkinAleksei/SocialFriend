package org.example.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.UserDto;
import org.example.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RequestMapping("/api/v1/social/register/")
@RestController
public class PublicUserController {
    private final UserService userService;

    @PostMapping("/signUp")
    public ResponseEntity<UserDto> signUp(
            @Valid @RequestBody RegistrationUserDto registrationUserDto
    ) {
        return ResponseEntity.ok().body(userService.signUp(registrationUserDto));
    }
}
