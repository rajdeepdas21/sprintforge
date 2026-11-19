package com.sprintforge.controller;

import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.dto.LoginResponse;
import com.sprintforge.dto.UserResponse;
import com.sprintforge.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerReturns201WhenRequestIsValid() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(
                5L,
                "Person",
                "person@example.com",
                OffsetDateTime.parse("2026-11-18T10:00:00Z")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Person",
                                    "email": "person@example.com",
                                    "password": "Password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("person@example.com"));
    }

    @Test
    void registerReturns400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Person",
                                    "email": "not-an-email",
                                    "password": "Password123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResponse("jwt-token", "Bearer"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "person@example.com",
                                    "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginReturns401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException(
                        "Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "person@example.com",
                                    "password": "WrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    @Test
    void registerReturns409WhenEmailIsAlreadyRegistered() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Person",
                                    "email": "person@example.com",
                                    "password": "Password123"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}