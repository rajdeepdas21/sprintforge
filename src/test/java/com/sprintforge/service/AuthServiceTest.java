package com.sprintforge.service;

import com.sprintforge.dto.LoginRequest;
import com.sprintforge.dto.LoginResponse;
import com.sprintforge.dto.RegisterRequest;
import com.sprintforge.dto.UserResponse;
import com.sprintforge.entity.User;
import com.sprintforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);

        authService = new AuthService(
                userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("Password123"))
                .thenReturn("hashed-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest(
                "  Person  ", " Person@Example.com ", "Password123");

        UserResponse response = authService.register(request);

        assertEquals("Person", response.name());
        assertEquals("person@example.com", response.email());
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerReturns409WhenEmailIsAlreadyUsed() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(new RegisterRequest(
                        "Person", "person@example.com", "Password123"))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User();
        user.setEmail("person@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateToken("person@example.com"))
                .thenReturn("jwt-token");

        LoginResponse response = authService.login(
                new LoginRequest(" Person@Example.com ", "Password123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest("missing@example.com", "Password123"))
        );

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setEmail("person@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-password"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "person@example.com", "WrongPassword"))
        );

        verify(jwtService, never()).generateToken(any());
    }
}