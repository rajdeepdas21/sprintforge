
package com.sprintforge.controller;

import com.sprintforge.dto.RegisterRequest;
import com.sprintforge.dto.UserResponse;
import com.sprintforge.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.sprintforge.dto.LoginRequest;
import com.sprintforge.dto.LoginResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
public LoginResponse login(
        @Valid @RequestBody LoginRequest request) {

    return authService.login(request);
}
}
