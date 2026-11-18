package com.sprintforge.controller;

import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.entity.Workspace;
import com.sprintforge.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceService workspaceService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void updateWorkspaceReturns200WhenRequestIsValid() throws Exception {
        Workspace updated = new Workspace();
        updated.setName("Updated workspace");
        updated.setOwnerId(10L);

        when(workspaceService.updateWorkspace(
                eq(1L), any(), eq(OWNER_EMAIL)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/workspaces/1")
                        .principal(ownerAuthentication())
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Updated workspace"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated workspace"));
    }

    @Test
    void updateWorkspaceReturns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(put("/api/workspaces/1")
                        .principal(ownerAuthentication())
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteWorkspaceReturns204WhenDeletionSucceeds() throws Exception {
        mockMvc.perform(delete("/api/workspaces/1")
                        .principal(ownerAuthentication()))
                .andExpect(status().isNoContent());

        verify(workspaceService).deleteWorkspace(1L, OWNER_EMAIL);
    }

    @Test
    void deleteWorkspaceReturns409WhenWorkspaceHasProjects()
            throws Exception {
        doThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delete the workspace's projects before deleting the workspace"))
                .when(workspaceService).deleteWorkspace(1L, OWNER_EMAIL);

        mockMvc.perform(delete("/api/workspaces/1")
                        .principal(ownerAuthentication()))
                .andExpect(status().isConflict());
    }

    // Supplies the authenticated email that the controller passes to the service.
    private UsernamePasswordAuthenticationToken ownerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                OWNER_EMAIL, null, List.of());
    }
}