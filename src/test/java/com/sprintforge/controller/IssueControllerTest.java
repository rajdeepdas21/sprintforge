
package com.sprintforge.controller;

import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.dto.UpdateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.service.IssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createIssueReturns201WhenRequestIsValid() throws Exception {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Implement login");
        issue.setDescription("Create login API");
        issue.setPriority("HIGH");

        when(issueService.createIssue(any(), any())).thenReturn(issue);

        String json = """
                {
                    "projectId": 1,
                    "title": "Implement login",
                    "description": "Create login API",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Implement login"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void createIssueReturns400WhenTitleIsMissing() throws Exception {
        String json = """
                {
                    "projectId": 1,
                    "description": "Create login API",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIssuesByProjectIdReturns200AndIssueList() throws Exception {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Implement login");
        issue.setPriority("HIGH");

        when(issueService.getIssuesByProjectId(eq(1L), any()))
                .thenReturn(List.of(issue));

        mockMvc.perform(get("/api/issues?projectId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Implement login"));
    }

    @Test
    void getIssuesByProjectIdReturns404WhenProjectDoesNotExist()
            throws Exception {
        when(issueService.getIssuesByProjectId(eq(9999L), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        mockMvc.perform(get("/api/issues?projectId=9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignIssueToSprintReturns200WhenRequestIsValid() throws Exception {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setSprintId(1L);
        issue.setTitle("Implement login");

        when(issueService.assignIssueToSprint(eq(1L), eq(1L), any()))
                .thenReturn(issue);

        String json = """
                {
                    "sprintId": 1
                }
                """;

        mockMvc.perform(patch("/api/issues/1/sprint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(1))
                .andExpect(jsonPath("$.title").value("Implement login"));
    }

    @Test
    void assignIssueToSprintReturns400WhenSprintIdIsMissing()
            throws Exception {
        mockMvc.perform(patch("/api/issues/1/sprint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignIssueToSprintReturns404WhenIssueDoesNotExist()
            throws Exception {
        when(issueService.assignIssueToSprint(eq(999L), eq(1L), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));

        String json = """
                {
                    "sprintId": 1
                }
                """;

        mockMvc.perform(patch("/api/issues/999/sprint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIssuesBySprintIdReturnsIssues() throws Exception {
        Issue issue = new Issue();
        issue.setTitle("Implement login");
        issue.setProjectId(1L);
        issue.setSprintId(1L);
        issue.setStatus("IN_PROGRESS");
        issue.setPriority("HIGH");

        when(issueService.getIssuesBySprintId(eq(1L), any()))
                .thenReturn(List.of(issue));

        mockMvc.perform(get("/api/issues?sprintId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Implement login"))
                .andExpect(jsonPath("$[0].sprintId").value(1));
    }

    @Test
    void getIssuesBySprintIdReturns404WhenSprintNotFound()
            throws Exception {
        when(issueService.getIssuesBySprintId(eq(9999L), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        mockMvc.perform(get("/api/issues?sprintId=9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateIssueReturnsUpdatedIssue() throws Exception {
        Issue issue = new Issue();
        issue.setTitle("Authentication API");
        issue.setDescription("JWT authentication");
        issue.setPriority("URGENT");

        when(issueService.updateIssue(
                eq(1L), any(UpdateIssueRequest.class), any()))
                .thenReturn(issue);

        String json = """
                {
                    "title": "Authentication API",
                    "description": "JWT authentication",
                    "priority": "URGENT"
                }
                """;

        mockMvc.perform(patch("/api/issues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Authentication API"))
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    @Test
    void updateIssueReturns400WhenTitleIsMissing() throws Exception {
        String json = """
                {
                    "description": "JWT authentication",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(patch("/api/issues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateIssueReturns404WhenIssueDoesNotExist()
            throws Exception {
        when(issueService.updateIssue(
                eq(9999L), any(UpdateIssueRequest.class), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));

        String json = """
                {
                    "title": "Authentication API"
                }
                """;

        mockMvc.perform(patch("/api/issues/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }
}
