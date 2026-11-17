package com.sprintforge.controller;

import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.dto.UpdateSprintRequest;
import com.sprintforge.entity.Sprint;
import com.sprintforge.service.SprintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SprintController.class)
@AutoConfigureMockMvc(addFilters = false)
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SprintService sprintService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void updateSprintStatusReturnsUpdatedSprint() throws Exception {
        Sprint sprint = new Sprint();
        sprint.setProjectId(1L);
        sprint.setName("Sprint 1");
        sprint.setStatus("ACTIVE");

        when(sprintService.updateSprintStatus(1L, "ACTIVE", null))
                .thenReturn(sprint);

        mockMvc.perform(patch("/api/sprints/1/status")
                        .contentType("application/json")
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value("Sprint 1"));
    }

    @Test
    void updateSprintStatusReturns400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/sprints/1/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSprintStatusReturns400WhenStatusIsInvalid() throws Exception {
        when(sprintService.updateSprintStatus(1L, "INVALID", null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid sprint status"));

        mockMvc.perform(patch("/api/sprints/1/status")
                        .contentType("application/json")
                        .content("""
                                {
                                    "status": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSprintStatusReturns404WhenSprintIsNotFound() throws Exception {
        when(sprintService.updateSprintStatus(9999L, "ACTIVE", null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        mockMvc.perform(patch("/api/sprints/9999/status")
                        .contentType("application/json")
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSprintStatusReturns403WhenAccessIsDenied() throws Exception {
        when(sprintService.updateSprintStatus(1L, "ACTIVE", null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(patch("/api/sprints/1/status")
                        .contentType("application/json")
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSprintByIdReturnsSprint() throws Exception {
        Sprint sprint = new Sprint();
        sprint.setProjectId(10L);
        sprint.setName("Sprint 1");
        sprint.setStatus("PLANNED");

        when(sprintService.getSprintById(1L, null)).thenReturn(sprint);

        mockMvc.perform(get("/api/sprints/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sprint 1"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void getSprintByIdReturns403WhenAccessIsDenied() throws Exception {
        when(sprintService.getSprintById(1L, null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/sprints/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSprintByIdReturns404WhenSprintIsNotFound() throws Exception {
        when(sprintService.getSprintById(9999L, null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        mockMvc.perform(get("/api/sprints/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSprintsByProjectIdReturnsSprints() throws Exception {
        Sprint sprint = new Sprint();
        sprint.setProjectId(10L);
        sprint.setName("Sprint 1");
        sprint.setStatus("PLANNED");

        when(sprintService.getSprintsByProjectId(10L, null))
                .thenReturn(List.of(sprint));

        mockMvc.perform(get("/api/sprints").param("projectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sprint 1"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    void getSprintsByProjectIdReturns403WhenAccessIsDenied() throws Exception {
        when(sprintService.getSprintsByProjectId(10L, null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/sprints").param("projectId", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSprintsByProjectIdReturns400WhenProjectIdIsMissing()
            throws Exception {
        mockMvc.perform(get("/api/sprints"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSprintReturns200WhenRequestIsValid() throws Exception {
        Sprint updatedSprint = new Sprint();
        updatedSprint.setProjectId(10L);
        updatedSprint.setName("Updated sprint");
        updatedSprint.setStatus("PLANNED");

        when(sprintService.updateSprint(
                eq(1L), any(UpdateSprintRequest.class), isNull()))
                .thenReturn(updatedSprint);

        mockMvc.perform(put("/api/sprints/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Updated sprint",
                                    "startDate": "2026-11-01",
                                    "endDate": "2026-11-14"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated sprint"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void updateSprintReturns400WhenNameIsMissing() throws Exception {
        mockMvc.perform(put("/api/sprints/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "startDate": "2026-11-01",
                                    "endDate": "2026-11-14"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSprintReturns403WhenAccessIsDenied() throws Exception {
        when(sprintService.updateSprint(
                eq(1L), any(UpdateSprintRequest.class), isNull()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can manage sprints"));

        mockMvc.perform(put("/api/sprints/1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Updated sprint"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSprintReturns204WhenDeletionSucceeds() throws Exception {
        // The mocked service does nothing by default, representing successful deletion.
        mockMvc.perform(delete("/api/sprints/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSprintReturns409WhenSprintHasIssues() throws Exception {
        doThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unassign this sprint's issues before deleting it"))
                .when(sprintService).deleteSprint(1L, null);

        mockMvc.perform(delete("/api/sprints/1"))
                .andExpect(status().isConflict());
    }
}