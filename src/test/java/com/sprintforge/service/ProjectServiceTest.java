
package com.sprintforge.service;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.WorkspaceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService projectService;

    private static final String EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;

    @Test
    void createProjectThrows404WhenWorkspaceDoesNotExist() {

        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL))
                .thenReturn(USER_ID);

        when(workspaceRepository.existsById(1L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.createProject(request, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void createProjectThrows409WhenProjectKeyAlreadyExists() {

        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL))
                .thenReturn(USER_ID);

        when(workspaceRepository.existsById(1L))
                .thenReturn(true);

        when(workspaceRepository.isOwnedByUser(1L, USER_ID))
                .thenReturn(true);

        when(projectRepository.existsByWorkspaceIdAndProjectKey(1L, "SFB"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.createProject(request, EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void createProjectSavesProjectWhenRequestIsValid() {

        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL))
                .thenReturn(USER_ID);

        when(workspaceRepository.existsById(1L))
                .thenReturn(true);

        when(workspaceRepository.isOwnedByUser(1L, USER_ID))
                .thenReturn(true);

        when(projectRepository.existsByWorkspaceIdAndProjectKey(1L, "SFB"))
                .thenReturn(false);

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.createProject(request, EMAIL);

        assertEquals(1L, result.getWorkspaceId());
        assertEquals("SprintForge Backend", result.getName());
        assertEquals("SFB", result.getProjectKey());

        verify(projectRepository).save(any(Project.class));
    }

    private CreateProjectRequest createRequest() {

        CreateProjectRequest request = new CreateProjectRequest();
        request.setWorkspaceId(1L);
        request.setName("SprintForge Backend");
        request.setProjectKey("SFB");

        return request;
    }
}
