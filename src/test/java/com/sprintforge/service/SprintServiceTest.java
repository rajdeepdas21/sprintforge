
package com.sprintforge.service;

import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SprintServiceTest {

    private SprintRepository sprintRepository;
    private ProjectRepository projectRepository;
    private SprintService sprintService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        sprintRepository = mock(SprintRepository.class);
        projectRepository = mock(ProjectRepository.class);
        userService = mock(UserService.class);

        when(userService.getUserIdByEmail("user@test.com"))
            .thenReturn(1L);

    when(projectRepository.isOwnedByUser(10L, 1L))
            .thenReturn(true);

        sprintService = new SprintService(
                sprintRepository,
                projectRepository,
                userService
        );
    }

    @Test
    void updateSprintStatusChangesPlannedToActive() {
        Sprint sprint = new Sprint();
        sprint.setStatus("PLANNED");
        sprint.setProjectId(10L);
        when(sprintRepository.findById(1L))
                .thenReturn(Optional.of(sprint));

        when(sprintRepository.save(sprint))
                .thenReturn(sprint);

        Sprint result =
                sprintService.updateSprintStatus(1L, "ACTIVE","user@test.com");

        assertEquals("ACTIVE", result.getStatus());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintStatusChangesActiveToCompleted() {
        Sprint sprint = new Sprint();
        sprint.setStatus("ACTIVE");
        sprint.setProjectId(10L);

        when(sprintRepository.findById(1L))
                .thenReturn(Optional.of(sprint));

        when(sprintRepository.save(sprint))
                .thenReturn(sprint);

        Sprint result =
                sprintService.updateSprintStatus(1L, "COMPLETED","user@test.com");

        assertEquals("COMPLETED", result.getStatus());
        verify(sprintRepository).save(sprint);
    }

    @Test
    void updateSprintStatusRejectsInvalidTransition() {
        Sprint sprint = new Sprint();
        sprint.setStatus("PLANNED");
        sprint.setProjectId(10L);

        when(sprintRepository.findById(1L))
                .thenReturn(Optional.of(sprint));

        assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        1L, "COMPLETED","user@test.com"  
                )
        );

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprintStatusRejectsInvalidStatus() {
        Sprint sprint = new Sprint();
sprint.setStatus("PLANNED");
sprint.setProjectId(10L);
when(sprintRepository.findById(1L))
        .thenReturn(Optional.of(sprint));
        assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        1L, "INVALID","user@test.com"
                )
        );

        verify(sprintRepository).findById(1L);
        verify(projectRepository).isOwnedByUser(10L, 1L);
    }

    @Test
    void updateSprintStatusReturns404WhenSprintDoesNotExist() {
        when(sprintRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sprintService.updateSprintStatus(
                        999L, "ACTIVE","user@test.com"
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
void updateSprintStatusRejectsUnauthorizedUser() {
    Sprint sprint = new Sprint();
    sprint.setStatus("PLANNED");
    sprint.setProjectId(10L);

    when(sprintRepository.findById(1L))
            .thenReturn(Optional.of(sprint));

    when(projectRepository.isOwnedByUser(10L, 1L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> sprintService.updateSprintStatus(
                    1L, "ACTIVE", "user@test.com"
            )
    );

    assertEquals(403, exception.getStatusCode().value());
    verify(sprintRepository, never()).save(any());
}
@Test
void getSprintById_shouldThrow403_whenAccessDenied() {

    Sprint sprint = new Sprint();
    sprint.setProjectId(10L);
    sprint.setStatus("PLANNED");

    when(sprintRepository.findById(1L))
            .thenReturn(Optional.of(sprint));

    when(projectRepository.isOwnedByUser(10L, 1L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> sprintService.getSprintById(1L, "user@test.com")
    );

    assertEquals(403, exception.getStatusCode().value());
}
@Test
void getSprintById_shouldReturnSprint_whenAuthorized() {

    Sprint sprint = new Sprint();
    sprint.setProjectId(10L);
    sprint.setName("Sprint 1");
    sprint.setStatus("PLANNED");

    when(sprintRepository.findById(1L))
            .thenReturn(Optional.of(sprint));

    Sprint result = sprintService.getSprintById(
            1L, "user@test.com"
    );

    assertEquals("Sprint 1", result.getName());
    assertEquals("PLANNED", result.getStatus());

    verify(projectRepository).isOwnedByUser(10L, 1L);
}
@Test
void getSprintById_shouldThrow404_whenSprintNotFound() {

    when(sprintRepository.findById(999L))
            .thenReturn(Optional.empty());

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> sprintService.getSprintById(
                    999L, "user@test.com"
            )
    );

    assertEquals(404, exception.getStatusCode().value());
}
@Test
void getSprintsByProjectId_shouldReturnSprints_whenAuthorized() {

    Sprint sprint = new Sprint();
    sprint.setProjectId(10L);
    sprint.setName("Sprint 1");
    sprint.setStatus("PLANNED");

    when(projectRepository.existsById(10L))
            .thenReturn(true);

    when(projectRepository.isOwnedByUser(10L, 1L))
            .thenReturn(true);

    when(sprintRepository.findByProjectId(10L))
            .thenReturn(java.util.List.of(sprint));

    java.util.List<Sprint> result =
            sprintService.getSprintsByProjectId(
                    10L, "user@test.com"
            );

    assertEquals(1, result.size());
    assertEquals("Sprint 1", result.get(0).getName());

    verify(projectRepository).isOwnedByUser(10L, 1L);
}
@Test
void getSprintsByProjectId_shouldThrow404_whenProjectNotFound() {

    when(projectRepository.existsById(999L))
            .thenReturn(false);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> sprintService.getSprintsByProjectId(
                    999L, "user@test.com"
            )
    );

    assertEquals(404, exception.getStatusCode().value());
}
}
