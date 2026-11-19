package com.sprintforge.config;

import com.sprintforge.service.JwtService;
import com.sprintforge.service.ProjectService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.sprintforge.controller.ProjectController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtSecurityIntegrationTest.WebSecurityInfrastructure.class
})
class JwtSecurityIntegrationTest {

    private static final String EMAIL = "owner@example.com";

    @Autowired
    private MockMvc mockMvc;

    // Mock token parsing while keeping the real JWT filter in the test.
    @MockitoBean
    private JwtService jwtService;

    // Prevent the controller test from connecting to the database.
    @MockitoBean
    private ProjectService projectService;

    @Test
    void protectedEndpointReturns401WhenBearerTokenIsMissing()
            throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }

    @Test
    void protectedEndpointReturns401WhenBearerTokenIsInvalid()
            throws Exception {
        when(jwtService.extractEmail("bad-token"))
                .thenThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        when(jwtService.extractEmail("valid-token")).thenReturn(EMAIL);
        when(projectService.getAllProjects(EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(projectService).getAllProjects(EMAIL);
    }

    // Supplies the security infrastructure omitted by @WebMvcTest.
    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class WebSecurityInfrastructure {
    }
}