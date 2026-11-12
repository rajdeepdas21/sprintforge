
package com.sprintforge.controller;

import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.entity.Sprint;
import com.sprintforge.service.SprintService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import java.util.List;
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
    void updateSprintStatus_shouldReturnUpdatedSprint() throws Exception {

        Sprint sprint = new Sprint();
        sprint.setProjectId(1L);
        sprint.setName("Sprint 1");
        sprint.setStatus("ACTIVE");

        when(sprintService.updateSprintStatus(1L, "ACTIVE",null))
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
    void updateSprintStatus_shouldReturn400_whenStatusMissing() throws Exception {

        mockMvc.perform(patch("/api/sprints/1/status")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSprintStatus_shouldReturn400_whenStatusInvalid() throws Exception {

        when(sprintService.updateSprintStatus(1L, "INVALID",null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid sprint status"));

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
    void updateSprintStatus_shouldReturn404_whenSprintNotFound() throws Exception {

        when(sprintService.updateSprintStatus(9999L, "ACTIVE",null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sprint not found"));

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
void getSprintById_shouldReturnSprint() throws Exception {

    Sprint sprint = new Sprint();
    sprint.setProjectId(10L);
    sprint.setName("Sprint 1");
    sprint.setStatus("PLANNED");

    when(sprintService.getSprintById(1L, null))
            .thenReturn(sprint);

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Sprint 1"))
            .andExpect(jsonPath("$.status").value("PLANNED"));
}
@Test
void getSprintsByProjectId_shouldReturnSprints() throws Exception {

    Sprint sprint = new Sprint();
    sprint.setProjectId(10L);
    sprint.setName("Sprint 1");
    sprint.setStatus("PLANNED");

    when(sprintService.getSprintsByProjectId(10L, null))
            .thenReturn(List.of(sprint));

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints")
                    .param("projectId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Sprint 1"))
            .andExpect(jsonPath("$[0].status").value("PLANNED"));
}
@Test
void getSprintById_shouldReturn403_whenAccessDenied() throws Exception {

    when(sprintService.getSprintById(1L, null))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"));

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints/1"))
            .andExpect(status().isForbidden());
}
@Test
void getSprintsByProjectId_shouldReturn403_whenAccessDenied() throws Exception {

    when(sprintService.getSprintsByProjectId(10L, null))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"));

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints")
                    .param("projectId", "10"))
            .andExpect(status().isForbidden());
}
@Test
void getSprintsByProjectId_shouldReturn400_whenProjectIdMissing() throws Exception {

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints"))
            .andExpect(status().isBadRequest());
}
@Test
void updateSprintStatus_shouldReturn403_whenAccessDenied() throws Exception {

    when(sprintService.updateSprintStatus(1L, "ACTIVE", null))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"));

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
void getSprintById_shouldReturn404_whenSprintNotFound() throws Exception {

    when(sprintService.getSprintById(9999L, null))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sprint not found"));

    mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/sprints/9999"))
            .andExpect(status().isNotFound());
}
}
