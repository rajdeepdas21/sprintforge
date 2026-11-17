package com.sprintforge.service;

import com.sprintforge.dto.UpdateSprintRequest;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SprintServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long WORKSPACE_ID = 100L;
    private static final String EMAIL = "user@test.com";

    private SprintRepository sprintRepository;
    private ProjectRepository projectRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private IssueRepository issueRepository;
    private UserService userService;
    private SprintService sprintService;

    @BeforeEach
    void setUp() {
        sprintRepository = mock(SprintRepository.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        issueRepository = mock(IssueRepository.class);
        userService = mock(UserService.class);

        // Most tests use an authorized OWNER; individual tests override the role.
        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRepository.isAccessibleByUser(PROJECT_ID, USER_ID))
                .thenReturn(true);
        when(projectRepository.findWorkspaceIdByProjectId(PROJECT_ID))
                .thenReturn(Optional.of(WORKSPACE_ID));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("OWNER"));

        // IssueRepository is required so SprintService can prevent deleting
        // sprints that still have issues assigned to them.
        sprintService = new SprintService(
                sprintRepository,
                projectRepository,
                workspaceMemberRepository,
                issueRepository,
                userService
        );
    }

    @Test
    void updateSprintStatusChangesPlannedToActive() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        Sprint result =
                sprintService.updateSprintStatus(1L, "ACTIVE", EMAIL);

        assertEquals("ACTIVE", result.getStatus());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintStatusChangesActiveToCompleted() {
        Sprint sprint = createSprint("ACTIVE");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        Sprint result =
                sprintService.updateSprintStatus(1L, "COMPLETED", EMAIL);

        assertEquals("COMPLETED", result.getStatus());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintStatusRejectsInvalidTransition() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        1L, "COMPLETED", EMAIL)
        );

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprintStatusRejectsInvalidStatus() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        1L, "INVALID", EMAIL)
        );

        verify(sprintRepository).findById(1L);
        verify(workspaceMemberRepository)
                .findRole(WORKSPACE_ID, USER_ID);
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprintStatusReturns404WhenSprintDoesNotExist() {
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        999L, "ACTIVE", EMAIL)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void updateSprintStatusRejectsUnauthorizedUser() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("MEMBER"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        1L, "ACTIVE", EMAIL)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprintSavesChangesWhenRequesterIsOwner() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        UpdateSprintRequest request = createUpdateRequest();

        Sprint result = sprintService.updateSprint(1L, request, EMAIL);

        assertEquals("Updated sprint", result.getName());
        assertEquals(LocalDate.of(2026, 11, 1), result.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 14), result.getEndDate());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintSavesChangesWhenRequesterIsAdmin() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("ADMIN"));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        UpdateSprintRequest request = createUpdateRequest();

        Sprint result = sprintService.updateSprint(1L, request, EMAIL);

        assertEquals("Updated sprint", result.getName());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintThrows403WhenRequesterIsMember() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("MEMBER"));

        UpdateSprintRequest request = createUpdateRequest();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprint(1L, request, EMAIL)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void deleteSprintThrows409WhenSprintHasIssues() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(issueRepository.existsBySprintId(1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.deleteSprint(1L, EMAIL)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(sprintRepository, never()).delete(any(Sprint.class));
    }

    @Test
    void deleteSprintThrows403WhenRequesterIsMember() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(workspaceMemberRepository.findRole(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of("MEMBER"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.deleteSprint(1L, EMAIL)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(sprintRepository, never()).delete(any(Sprint.class));
    }

    @Test
    void getSprintByIdThrows403WhenAccessDenied() {
        Sprint sprint = createSprint("PLANNED");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
        when(projectRepository.isAccessibleByUser(PROJECT_ID, USER_ID))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.getSprintById(1L, EMAIL)
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void getSprintByIdReturnsSprintWhenAuthorized() {
        Sprint sprint = createSprint("PLANNED");
        sprint.setName("Sprint 1");
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        Sprint result = sprintService.getSprintById(1L, EMAIL);

        assertEquals("Sprint 1", result.getName());
        assertEquals("PLANNED", result.getStatus());
        verify(projectRepository)
                .isAccessibleByUser(PROJECT_ID, USER_ID);
    }

    @Test
    void getSprintByIdThrows404WhenSprintNotFound() {
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.getSprintById(999L, EMAIL)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getSprintsByProjectIdReturnsSprintsWhenAuthorized() {
        Sprint sprint = createSprint("PLANNED");
        sprint.setName("Sprint 1");

        when(sprintRepository.findByProjectId(PROJECT_ID))
                .thenReturn(List.of(sprint));

        List<Sprint> result =
                sprintService.getSprintsByProjectId(PROJECT_ID, EMAIL);

        assertEquals(1, result.size());
        assertEquals("Sprint 1", result.get(0).getName());
        verify(projectRepository)
                .isAccessibleByUser(PROJECT_ID, USER_ID);
    }

    @Test
    void getSprintsByProjectIdThrows404WhenProjectNotFound() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.getSprintsByProjectId(999L, EMAIL)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    // Builds the request used by the sprint update tests.
    private UpdateSprintRequest createUpdateRequest() {
        UpdateSprintRequest request = new UpdateSprintRequest();
        request.setName("Updated sprint");
        request.setStartDate(LocalDate.of(2026, 11, 1));
        request.setEndDate(LocalDate.of(2026, 11, 14));
        return request;
    }

    // Creates a sprint attached to the project used in this test class.
    private Sprint createSprint(String status) {
        Sprint sprint = new Sprint();
        sprint.setProjectId(PROJECT_ID);
        sprint.setStatus(status);
        return sprint;
    }
}