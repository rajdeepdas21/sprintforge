package com.sprintforge.service;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.dto.UpdateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.AssignIssueToUserRequest;
import com.sprintforge.entity.User;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IssueServiceTest {

    private IssueRepository issueRepository;
    private ProjectRepository projectRepository;
    private SprintRepository sprintRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private UserService userService;
    private IssueService issueService;

    private static final String EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 100L;

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        projectRepository = mock(ProjectRepository.class);
        sprintRepository = mock(SprintRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        userService = mock(UserService.class);

        issueService = new IssueService(
                issueRepository,
                projectRepository,
                sprintRepository,
                workspaceMemberRepository,
                userService
        );

        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    }

    private void authorizeProject(Long projectId) {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectRepository.isAccessibleByUser(projectId, USER_ID))
                .thenReturn(true);
        when(projectRepository.findWorkspaceIdByProjectId(projectId))
                .thenReturn(Optional.of(WORKSPACE_ID));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("MEMBER"));
    }

    @Test
    void createIssueThrows404WhenProjectDoesNotExist() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(999L);
        request.setTitle("Implement login");

        when(projectRepository.findWorkspaceIdByProjectId(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.createIssue(request, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void createIssueThrows403WhenUserIsNotAMember() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(1L);
        request.setTitle("Implement login");

        when(projectRepository.findWorkspaceIdByProjectId(1L))
                .thenReturn(Optional.of(WORKSPACE_ID));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.createIssue(request, EMAIL)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void createIssueSavesIssueWhenRequestIsValid() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setProjectId(1L);
        request.setTitle("Implement login");
        request.setDescription("Create login API");
        request.setPriority("HIGH");

        authorizeProject(1L);
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Issue result = issueService.createIssue(request, EMAIL);

        assertEquals(1L, result.getProjectId());
        assertEquals("Implement login", result.getTitle());
        assertEquals("Create login API", result.getDescription());
        assertEquals("HIGH", result.getPriority());
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void getIssuesByProjectIdReturnsIssuesWhenAuthorized() {
        authorizeProject(1L);

        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Implement login");

        when(issueRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(issue)));

        Page<Issue> result = issueService.getIssuesByProjectId(
                1L, EMAIL, null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals("Implement login", result.getContent().get(0).getTitle());
    }

    @Test
    void getIssuesByProjectIdThrows404WhenProjectDoesNotExist() {
        when(projectRepository.existsById(9999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.getIssuesByProjectId(
                        9999L, EMAIL, null, null, null, null,
                        PageRequest.of(0, 10))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(issueRepository, never()).findAll(
                any(Specification.class), any(Pageable.class));
    }

    @Test
    void getIssuesByProjectIdThrows403WhenUnauthorized() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectRepository.isAccessibleByUser(1L, USER_ID))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.getIssuesByProjectId(
                        1L, EMAIL, null, null, null, null,
                        PageRequest.of(0, 10))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(issueRepository, never()).findAll(
                any(Specification.class), any(Pageable.class));
    }

    @Test
    void updateIssueStatusUpdatesIssueWhenValid() {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setStatus("TODO");

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Issue result =
                issueService.updateIssueStatus(1L, "IN_PROGRESS", EMAIL);

        assertEquals("IN_PROGRESS", result.getStatus());
        verify(issueRepository).save(issue);
    }

    @Test
    void updateIssueStatusThrows400WhenStatusIsInvalid() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssueStatus(
                        1L, "SUPER_DONE", EMAIL)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(issueRepository, never()).findById(any());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void updateIssueStatusThrows404WhenIssueDoesNotExist() {
        when(issueRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssueStatus(999L, "DONE", EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void assignIssueToSprintSuccessfully() {
        Issue issue = new Issue();
        issue.setProjectId(1L);

        Sprint sprint = new Sprint();
        sprint.setProjectId(1L);

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Issue result = issueService.assignIssueToSprint(1L, 1L, EMAIL);

        assertEquals(1L, result.getSprintId());
        verify(issueRepository).save(issue);
    }

    @Test
    void assignIssueToSprintThrows404WhenSprintDoesNotExist() {
        Issue issue = new Issue();
        issue.setProjectId(1L);

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.assignIssueToSprint(1L, 999L, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void assignIssueToSprintThrows400WhenProjectsDoNotMatch() {
        Issue issue = new Issue();
        issue.setProjectId(1L);

        Sprint sprint = new Sprint();
        sprint.setProjectId(2L);

        authorizeProject(1L);
        authorizeProject(2L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.assignIssueToSprint(1L, 1L, EMAIL)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(issueRepository, never()).save(any());
    }

    @Test
    void updateIssueUpdatesTitleDescriptionAndPriority() {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Login API");
        issue.setDescription("Old description");
        issue.setPriority("HIGH");

        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setTitle("Authentication API");
        request.setDescription("JWT authentication");
        request.setPriority("URGENT");

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);

        Issue result = issueService.updateIssue(1L, request, EMAIL);

        assertEquals("Authentication API", result.getTitle());
        assertEquals("JWT authentication", result.getDescription());
        assertEquals("URGENT", result.getPriority());
    }

    @Test
    void updateIssuePreservesDescriptionAndPriorityWhenOmitted() {
        Issue issue = new Issue();
        issue.setProjectId(1L);
        issue.setTitle("Old title");
        issue.setDescription("Keep description");
        issue.setPriority("HIGH");

        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setTitle("New title");

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);

        Issue result = issueService.updateIssue(1L, request, EMAIL);

        assertEquals("New title", result.getTitle());
        assertEquals("Keep description", result.getDescription());
        assertEquals("HIGH", result.getPriority());
    }

    @Test
    void updateIssueRejectsInvalidPriority() {
        Issue issue = new Issue();
        issue.setProjectId(1L);

        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setTitle("Authentication API");
        request.setPriority("CRITICAL");

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssue(1L, request, EMAIL)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateIssueThrows404WhenIssueDoesNotExist() {
        UpdateIssueRequest request = new UpdateIssueRequest();
        request.setTitle("Authentication API");

        when(issueRepository.findById(9999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.updateIssue(9999L, request, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteIssueDeletesAuthorizedIssue() {
        Issue issue = new Issue();
        issue.setProjectId(1L);

        authorizeProject(1L);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        issueService.deleteIssue(1L, EMAIL);

        verify(issueRepository).delete(issue);
    }

    @Test
    void deleteIssueThrows404WhenIssueDoesNotExist() {
        when(issueRepository.findById(9999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> issueService.deleteIssue(9999L, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(issueRepository, never()).delete(any(Issue.class));
    }


    @Test
void assignIssueToUserAssignsWorkspaceMember() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    User assignee = mock(User.class);
    when(assignee.getId()).thenReturn(2L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(userService.getUserByEmail("member@example.com"))
            .thenReturn(assignee);
    when(workspaceMemberRepository
            .existsByWorkspaceIdAndUserId(WORKSPACE_ID, 2L))
            .thenReturn(true);
    when(issueRepository.save(any(Issue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Issue result = issueService.assignIssueToUser(
            1L, new AssignIssueToUserRequest("member@example.com"), EMAIL);

    assertEquals(2L, result.getAssigneeId());
    verify(issueRepository).save(issue);
}

@Test
void assignIssueToUserRejectsNonMember() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    User assignee = mock(User.class);
    when(assignee.getId()).thenReturn(2L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(userService.getUserByEmail("outsider@example.com"))
            .thenReturn(assignee);
    when(workspaceMemberRepository
            .existsByWorkspaceIdAndUserId(WORKSPACE_ID, 2L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToUser(
                    1L,
                    new AssignIssueToUserRequest("outsider@example.com"),
                    EMAIL)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(issueRepository, never()).save(any());
}

@Test
void unassignIssueClearsAssignee() {
    Issue issue = new Issue();
    issue.setProjectId(1L);
    issue.setAssigneeId(2L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(issueRepository.save(any(Issue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Issue result = issueService.unassignIssue(1L, EMAIL);

    assertNull(result.getAssigneeId());
    verify(issueRepository).save(issue);
}

@Test
void assignIssueToUserRejectsSelfAssignment() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    User assignee = mock(User.class);
    when(assignee.getId()).thenReturn(USER_ID);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(userService.getUserByEmail(EMAIL)).thenReturn(assignee);
    when(workspaceMemberRepository
            .existsByWorkspaceIdAndUserId(WORKSPACE_ID, USER_ID))
            .thenReturn(true);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToUser(
                    1L,
                    new AssignIssueToUserRequest(EMAIL),
                    EMAIL)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(issueRepository, never()).save(any());
}

@Test
void assignIssueToUserAssignsAnotherWorkspaceMember() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    User assignee = mock(User.class);
    when(assignee.getId()).thenReturn(2L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(userService.getUserByEmail("other@example.com"))
            .thenReturn(assignee);
    when(workspaceMemberRepository
            .existsByWorkspaceIdAndUserId(WORKSPACE_ID, 2L))
            .thenReturn(true);
    when(issueRepository.save(any(Issue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Issue result = issueService.assignIssueToUser(
            1L,
            new AssignIssueToUserRequest("other@example.com"),
            EMAIL);

    assertEquals(2L, result.getAssigneeId());
    verify(issueRepository).save(issue);
}

@Test
void assignIssueToUserRejectsAssigneeOutsideWorkspace() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    User assignee = mock(User.class);
    when(assignee.getId()).thenReturn(2L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(userService.getUserByEmail("outsider@example.com"))
            .thenReturn(assignee);
    when(workspaceMemberRepository
            .existsByWorkspaceIdAndUserId(WORKSPACE_ID, 2L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToUser(
                    1L,
                    new AssignIssueToUserRequest("outsider@example.com"),
                    EMAIL)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(issueRepository, never()).save(any());
}

@Test
void assignIssueToUserRejectsViewer() {
    Issue issue = new Issue();
    issue.setProjectId(1L);

    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(projectRepository.findWorkspaceIdByProjectId(1L))
            .thenReturn(Optional.of(WORKSPACE_ID));
    when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
            .thenReturn(Optional.of("VIEWER"));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> issueService.assignIssueToUser(
                    1L,
                    new AssignIssueToUserRequest("other@example.com"),
                    EMAIL)
    );

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verify(userService, never()).getUserByEmail(any());
    verify(issueRepository, never()).save(any());
}
@Test
void unassignIssueFromSprintClearsSprintId() {
    Issue issue = new Issue();
    issue.setProjectId(1L);
    issue.setSprintId(3L);

    authorizeProject(1L);
    when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
    when(issueRepository.save(any(Issue.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Issue result = issueService.unassignIssueFromSprint(1L, EMAIL);

    assertNull(result.getSprintId());
    verify(issueRepository).save(issue);
}
}