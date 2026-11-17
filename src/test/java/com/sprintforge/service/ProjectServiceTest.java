package com.sprintforge.service;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import com.sprintforge.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.sprintforge.dto.UpdateProjectRequest;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.SprintRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserService userService;

    // These repositories are used to prevent deleting projects with child records.
@Mock
private SprintRepository sprintRepository;

@Mock
private IssueRepository issueRepository;

    @InjectMocks
    private ProjectService projectService;

    private static final String EMAIL = "test@example.com";
    private static final Long USER_ID = 1L;


    @Test
void updateProjectSavesChangesWhenRequesterIsAdmin() {
    Project project = new Project();
    project.setWorkspaceId(1L);
    project.setName("Old name");
    project.setProjectKey("OLD");

    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated backend");
    request.setProjectKey("SFB");

    when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    when(workspaceMemberRepository.findRole(1L, USER_ID))
            .thenReturn(Optional.of("ADMIN"));
    when(projectRepository.existsByWorkspaceIdAndProjectKeyAndIdNot(
            1L, "SFB", 10L)).thenReturn(false);
    when(projectRepository.save(project)).thenReturn(project);

    Project result = projectService.updateProject(10L, request, EMAIL);

    assertEquals("Updated backend", result.getName());
    assertEquals("SFB", result.getProjectKey());
    verify(projectRepository).save(project);
}

@Test
void updateProjectThrows409WhenKeyAlreadyBelongsToAnotherProject() {
    Project project = new Project();
    project.setWorkspaceId(1L);

    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated backend");
    request.setProjectKey("SFB");

    when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    when(workspaceMemberRepository.findRole(1L, USER_ID))
            .thenReturn(Optional.of("OWNER"));
    when(projectRepository.existsByWorkspaceIdAndProjectKeyAndIdNot(
            1L, "SFB", 10L)).thenReturn(true);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> projectService.updateProject(10L, request, EMAIL)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verify(projectRepository, never()).save(any(Project.class));
}

@Test
void updateProjectThrows403WhenRequesterIsMember() {
    Project project = new Project();
    project.setWorkspaceId(1L);

    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated backend");
    request.setProjectKey("SFB");

    when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    when(workspaceMemberRepository.findRole(1L, USER_ID))
            .thenReturn(Optional.of("MEMBER"));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> projectService.updateProject(10L, request, EMAIL)
    );

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verify(projectRepository, never()).save(any(Project.class));
}

@Test
void deleteProjectThrows409WhenProjectHasSprints() {
    Project project = new Project();
    project.setWorkspaceId(1L);

    when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    when(workspaceMemberRepository.findRole(1L, USER_ID))
            .thenReturn(Optional.of("OWNER"));
    when(sprintRepository.existsByProjectId(10L)).thenReturn(true);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> projectService.deleteProject(10L, EMAIL)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verify(projectRepository, never()).delete(any(Project.class));
}

@Test
void deleteProjectThrows403WhenRequesterIsMember() {
    Project project = new Project();
    project.setWorkspaceId(1L);

    when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
    when(workspaceMemberRepository.findRole(1L, USER_ID))
            .thenReturn(Optional.of("MEMBER"));

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> projectService.deleteProject(10L, EMAIL)
    );

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verify(projectRepository, never()).delete(any(Project.class));
}

    @Test
    void createProjectThrows404WhenWorkspaceDoesNotExist() {
        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
        when(workspaceRepository.existsById(1L)).thenReturn(false);

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

        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.findRole(1L, USER_ID))
                .thenReturn(Optional.of("OWNER"));
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
    void createProjectThrows403WhenRequesterIsMember() {
        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.findRole(1L, USER_ID))
                .thenReturn(Optional.of("MEMBER"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> projectService.createProject(request, EMAIL)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(projectRepository, never())
                .existsByWorkspaceIdAndProjectKey(1L, "SFB");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void createProjectSavesProjectWhenRequesterIsOwner() {
        CreateProjectRequest request = createRequest();

        when(userService.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.findRole(1L, USER_ID))
                .thenReturn(Optional.of("OWNER"));
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