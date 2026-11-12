
package com.sprintforge.service;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.service.IssueService;
import com.sprintforge.entity.Sprint;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.sprintforge.dto.UpdateIssueRequest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
class IssueServiceTest {

    private IssueRepository issueRepository;
    private ProjectRepository projectRepository;
    private IssueService issueService;
    private SprintRepository sprintRepository;

    @BeforeEach
void setUp() {
    issueRepository = mock(IssueRepository.class);
    projectRepository = mock(ProjectRepository.class);
    sprintRepository = mock(SprintRepository.class);

    issueService = new IssueService(
            issueRepository,
            projectRepository,
            sprintRepository
    );
}

    @Test
    void createIssueThrows404WhenProjectDoesNotExist() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(999L);
        request.setTitle("Implement user login");

        when(projectRepository.existsById(999L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.createIssue(request)
        );

        assertEquals(404, exception.getStatusCode().value());

        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    void createIssueSavesIssueWhenRequestIsValid() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(1L);
        request.setTitle("Implement user login");
        request.setDescription("Create the login API");
        request.setPriority("HIGH");

        when(projectRepository.existsById(1L))
                .thenReturn(true);

        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Issue result = issueService.createIssue(request);

        assertEquals(1L, result.getProjectId());
        assertEquals("Implement user login", result.getTitle());
        assertEquals("Create the login API", result.getDescription());
        assertEquals("HIGH", result.getPriority());

        verify(issueRepository).save(any(Issue.class));
    }

    
    @Test
    void getIssuesByProjectIdReturnsIssuesWhenProjectExists() {
        when(projectRepository.existsById(1L)).thenReturn(true);
    
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Implement user login");
    
        when(issueRepository.findByProjectId(1L))
                .thenReturn(java.util.List.of(issue));
    
        java.util.List<Issue> result = issueService.getIssuesByProjectId(1L);
    
        assertEquals(1, result.size());
        assertEquals("Implement user login", result.get(0).getTitle());
        verify(issueRepository).findByProjectId(1L);
    }
    
    @Test
    void getIssuesByProjectIdThrows404WhenProjectDoesNotExist() {
        when(projectRepository.existsById(9999L)).thenReturn(false);
    
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.getIssuesByProjectId(9999L)
        );
    
        assertEquals(404, exception.getStatusCode().value());
        verify(issueRepository, never()).findByProjectId(anyLong());
    }

    @Test
    void updateIssueStatusUpdatesIssueWhenValid() {
        Issue issue = new Issue();
        issue.setTitle("Implement user login");
        issue.setStatus("TODO");
    
        when(issueRepository.findById(1L))
                .thenReturn(java.util.Optional.of(issue));
    
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    
        Issue result = issueService.updateIssueStatus(1L, "IN_PROGRESS");
    
        assertEquals("IN_PROGRESS", result.getStatus());
    
        verify(issueRepository).findById(1L);
        verify(issueRepository).save(issue);
    }

    @Test
    void updateIssueStatusThrows400WhenStatusIsInvalid() {
    
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssueStatus(1L, "SUPER_DONE")
        );
    
        assertEquals(400, exception.getStatusCode().value());
    
        verify(issueRepository, never()).findById(anyLong());
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    void updateIssueStatusThrows404WhenIssueDoesNotExist() {
    
        when(issueRepository.findById(999L))
                .thenReturn(java.util.Optional.empty());
    
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssueStatus(999L, "DONE")
        );
    
        assertEquals(404, exception.getStatusCode().value());
    
        verify(issueRepository).findById(999L);
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
void assignIssueToSprintSuccessfully() {
    Issue issue = new Issue();
    issue.setProjectId(1L);
    issue.setTitle("Implement login");

    Sprint sprint = new Sprint();
    sprint.setProjectId(1L);
    sprint.setName("Sprint 1");

    when(issueRepository.findById(1L))
            .thenReturn(java.util.Optional.of(issue));

    when(sprintRepository.findById(1L))
            .thenReturn(java.util.Optional.of(sprint));

    when(issueRepository.save(any(Issue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Issue result = issueService.assignIssueToSprint(1L, 1L);

    assertEquals(1L, result.getSprintId());

    verify(issueRepository).save(issue);
}

@Test
void assignIssueToSprintThrows404WhenSprintDoesNotExist() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    when(issueRepository.findById(1L))
            .thenReturn(java.util.Optional.of(issue));

    when(sprintRepository.findById(999L))
            .thenReturn(java.util.Optional.empty());

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToSprint(1L, 999L)
    );

    assertEquals(404, exception.getStatusCode().value());

    verify(issueRepository, never()).save(any(Issue.class));
}

@Test
void assignIssueToSprintThrows400WhenProjectsDoNotMatch() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    Sprint sprint = new Sprint();
    sprint.setProjectId(2L);

    when(issueRepository.findById(1L))
            .thenReturn(java.util.Optional.of(issue));

    when(sprintRepository.findById(1L))
            .thenReturn(java.util.Optional.of(sprint));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToSprint(1L, 1L)
    );

    assertEquals(400, exception.getStatusCode().value());

    verify(issueRepository, never()).save(any(Issue.class));
}


@Test
void updateIssue_shouldUpdateTitleDescriptionAndPriority() {

    Issue issue = new Issue();
    issue.setTitle("Login API");
    issue.setDescription("Old description");
    issue.setPriority("HIGH");

    UpdateIssueRequest request = new UpdateIssueRequest();
    request.setTitle("Authentication API");
    request.setDescription("JWT authentication");
    request.setPriority("URGENT");

    when(issueRepository.findById(1L))
            .thenReturn(Optional.of(issue));

    when(issueRepository.save(issue))
            .thenReturn(issue);

    Issue updatedIssue = issueService.updateIssue(1L, request);

    assertEquals("Authentication API", updatedIssue.getTitle());
    assertEquals("JWT authentication", updatedIssue.getDescription());
    assertEquals("URGENT", updatedIssue.getPriority());

    verify(issueRepository).save(issue);
}

@Test
void updateIssue_shouldPreserveDescriptionAndPriority_whenOmitted() {
    Issue issue = new Issue();
    issue.setTitle("Old title");
    issue.setDescription("Keep this description");
    issue.setPriority("HIGH");

    UpdateIssueRequest request = new UpdateIssueRequest();
    request.setTitle("New title");
    // Description and priority intentionally omitted

    when(issueRepository.findById(1L))
            .thenReturn(Optional.of(issue));
    when(issueRepository.save(issue))
            .thenReturn(issue);

    Issue updatedIssue = issueService.updateIssue(1L, request);

    assertEquals("New title", updatedIssue.getTitle());
    assertEquals("Keep this description", updatedIssue.getDescription());
    assertEquals("HIGH", updatedIssue.getPriority());
}


@Test
void updateIssue_shouldRejectInvalidPriority() {
    Issue issue = new Issue();

    UpdateIssueRequest request = new UpdateIssueRequest();
    request.setTitle("Authentication API");
    request.setPriority("CRITICAL");

    when(issueRepository.findById(1L))
            .thenReturn(Optional.of(issue));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.updateIssue(1L, request)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
}


@Test
void updateIssue_shouldThrowNotFound_whenIssueDoesNotExist() {
    UpdateIssueRequest request = new UpdateIssueRequest();
    request.setTitle("Authentication API");

    when(issueRepository.findById(9999L))
            .thenReturn(Optional.empty());

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.updateIssue(9999L, request)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
}

@Test
void deleteIssue_shouldDelete_whenIssueExists() {
    when(issueRepository.existsById(1L))
            .thenReturn(true);

    issueService.deleteIssue(1L);

    verify(issueRepository).deleteById(1L);
}

@Test
void deleteIssue_shouldThrowNotFound_whenIssueDoesNotExist() {
    when(issueRepository.existsById(9999L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.deleteIssue(9999L)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

    verify(issueRepository, never()).deleteById(9999L);
}
// @Test
// void diagnosticTest() {
//     System.out.println("DIAGNOSTIC TEST EXECUTED");
// }
}
