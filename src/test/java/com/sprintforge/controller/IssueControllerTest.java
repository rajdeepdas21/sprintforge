
package com.sprintforge.controller;
import com.sprintforge.entity.Issue;
import com.sprintforge.service.IssueService;
import com.sprintforge.dto.UpdateIssueRequest;
import com.sprintforge.config.JwtAuthenticationFilter;
import com.sprintforge.controller.IssueController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

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
        issue.setTitle("Implement user login");
        issue.setDescription("Create the login API");
        issue.setPriority("HIGH");

        when(issueService.createIssue(any()))
                .thenReturn(issue);

        String requestJson = """
                {
                    "projectId": 1,
                    "title": "Implement user login",
                    "description": "Create the login API",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("Implement user login"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void createIssueReturns400WhenTitleIsMissing() throws Exception {
        String requestJson = """
                {
                    "projectId": 1,
                    "description": "Create the login API",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIssuesByProjectIdReturns200AndIssueList() throws Exception {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Implement user login");
        issue.setPriority("HIGH");

        when(issueService.getIssuesByProjectId(1L))
                .thenReturn(java.util.List.of(issue));

        mockMvc.perform(get("/api/issues?projectId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title")
                        .value("Implement user login"));
    }

    
    @Test
    void getIssuesByProjectIdReturns404WhenProjectDoesNotExist()
            throws Exception {
        when(issueService.getIssuesByProjectId(9999L))
                .thenThrow(
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Project not found"
                        )
                );
    
        mockMvc.perform(get("/api/issues?projectId=9999"))
                .andExpect(status().isNotFound());
    }

    @Test
void assignIssueToSprintReturns200WhenRequestIsValid() throws Exception {
    Issue issue = new Issue();
    issue.setProjectId(1L);
    issue.setSprintId(1L);
    issue.setTitle("Implement user login");

    when(issueService.assignIssueToSprint(1L, 1L))
            .thenReturn(issue);

    String requestJson = """
            {
                "sprintId": 1
            }
            """;

    mockMvc.perform(patch("/api/issues/1/sprint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sprintId").value(1))
            .andExpect(jsonPath("$.title")
                    .value("Implement user login"));
}

@Test
void assignIssueToSprintReturns400WhenSprintIdIsMissing()
        throws Exception {

    String requestJson = """
            {}
            """;

    mockMvc.perform(patch("/api/issues/1/sprint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isBadRequest());
}

@Test
void assignIssueToSprintReturns404WhenIssueDoesNotExist()
        throws Exception {

    when(issueService.assignIssueToSprint(999L, 1L))
            .thenThrow(
                    new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND,
                            "Issue not found"
                    )
            );

    String requestJson = """
            {
                "sprintId": 1
            }
            """;

    mockMvc.perform(patch("/api/issues/999/sprint")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isNotFound());
}

@Test
void getIssuesBySprintId_shouldReturnIssues() throws Exception {

    Issue issue = new Issue();
    issue.setTitle("Implement user login");
    issue.setProjectId(1L);
    issue.setSprintId(1L);
    issue.setStatus("IN_PROGRESS");
    issue.setPriority("HIGH");

    when(issueService.getIssuesBySprintId(1L))
            .thenReturn(List.of(issue));

    mockMvc.perform(get("/api/issues?sprintId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title")
                    .value("Implement user login"))
            .andExpect(jsonPath("$[0].sprintId")
                    .value(1));
}
 
@Test
void getIssuesBySprintId_shouldReturn404_whenSprintNotFound()
        throws Exception {

    when(issueService.getIssuesBySprintId(9999L))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sprint not found"
            ));

    mockMvc.perform(get("/api/issues?sprintId=9999"))
            .andExpect(status().isNotFound());
}


@Test
void updateIssue_shouldReturnUpdatedIssue() throws Exception {
    Issue issue = new Issue();
    issue.setTitle("Authentication API");
    issue.setDescription("JWT authentication");
    issue.setPriority("URGENT");

    when(issueService.updateIssue(eq(1L), any(UpdateIssueRequest.class)))
            .thenReturn(issue);

    String requestJson = """
            {
                "title": "Authentication API",
                "description": "JWT authentication",
                "priority": "URGENT"
            }
            """;

    mockMvc.perform(patch("/api/issues/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Authentication API"))
            .andExpect(jsonPath("$.priority").value("URGENT"));
}

@Test
void updateIssue_shouldReturn400_whenTitleIsMissing()
        throws Exception {

    String requestJson = """
            {
                "description": "JWT authentication",
                "priority": "HIGH"
            }
            """;

    mockMvc.perform(patch("/api/issues/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isBadRequest());
}

@Test
void updateIssue_shouldReturn404_whenIssueDoesNotExist()
        throws Exception {

    when(issueService.updateIssue(
            eq(9999L),
            any(UpdateIssueRequest.class)
    )).thenThrow(new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Issue not found"
    ));

    String requestJson = """
            {
                "title": "Authentication API"
            }
            """;

    mockMvc.perform(patch("/api/issues/9999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isNotFound());
}

}
