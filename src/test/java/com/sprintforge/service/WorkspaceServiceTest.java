package com.sprintforge.service;

import com.sprintforge.dto.UpdateWorkspaceMemberRoleRequest;
import com.sprintforge.entity.WorkspaceMember;
import com.sprintforge.repository.WorkspaceMemberRepository;
import com.sprintforge.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WorkspaceServiceTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Long ADMIN_ID = 20L;
    private static final Long MEMBER_ID = 30L;
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private UserService userService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        userService = mock(UserService.class);

        workspaceService = new WorkspaceService(
                workspaceRepository,
                userService,
                workspaceMemberRepository);

        when(workspaceRepository.existsById(WORKSPACE_ID))
                .thenReturn(true);
        when(userService.getUserIdByEmail(OWNER_EMAIL))
                .thenReturn(OWNER_ID);
        when(userService.getUserIdByEmail(ADMIN_EMAIL))
                .thenReturn(ADMIN_ID);
    }

    @Test
    void ownerCanPromoteMemberToAdmin() {
        WorkspaceMember owner =
                new WorkspaceMember(WORKSPACE_ID, OWNER_ID, "OWNER");
        WorkspaceMember member =
                new WorkspaceMember(WORKSPACE_ID, MEMBER_ID, "MEMBER");

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, MEMBER_ID))
                .thenReturn(Optional.of(member));
        when(workspaceMemberRepository.save(member)).thenReturn(member);

        WorkspaceMember result =
                workspaceService.updateWorkspaceMemberRole(
                        WORKSPACE_ID,
                        MEMBER_ID,
                        new UpdateWorkspaceMemberRoleRequest("ADMIN"),
                        OWNER_EMAIL);

        assertEquals("ADMIN", result.getRole());
        verify(workspaceMemberRepository).save(member);
    }

    @Test
    void adminCannotPromoteMemberToAdmin() {
        WorkspaceMember admin =
                new WorkspaceMember(WORKSPACE_ID, ADMIN_ID, "ADMIN");
        WorkspaceMember member =
                new WorkspaceMember(WORKSPACE_ID, MEMBER_ID, "MEMBER");

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, ADMIN_ID))
                .thenReturn(Optional.of(admin));
        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, MEMBER_ID))
                .thenReturn(Optional.of(member));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workspaceService.updateWorkspaceMemberRole(
                        WORKSPACE_ID,
                        MEMBER_ID,
                        new UpdateWorkspaceMemberRoleRequest("ADMIN"),
                        ADMIN_EMAIL));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void ownerCannotChangeOwnerRoleWithThisEndpoint() {
        WorkspaceMember owner =
                new WorkspaceMember(WORKSPACE_ID, OWNER_ID, "OWNER");

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, OWNER_ID))
                .thenReturn(Optional.of(owner));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workspaceService.updateWorkspaceMemberRole(
                        WORKSPACE_ID,
                        OWNER_ID,
                        new UpdateWorkspaceMemberRoleRequest("MEMBER"),
                        OWNER_EMAIL));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void adminCanRemoveMember() {
        WorkspaceMember admin =
                new WorkspaceMember(WORKSPACE_ID, ADMIN_ID, "ADMIN");
        WorkspaceMember member =
                new WorkspaceMember(WORKSPACE_ID, MEMBER_ID, "MEMBER");

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, ADMIN_ID))
                .thenReturn(Optional.of(admin));
        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, MEMBER_ID))
                .thenReturn(Optional.of(member));

        workspaceService.removeWorkspaceMember(
                WORKSPACE_ID, MEMBER_ID, ADMIN_EMAIL);

        verify(workspaceMemberRepository).delete(member);
    }

    @Test
    void adminCannotRemoveAnotherAdmin() {
        WorkspaceMember admin =
                new WorkspaceMember(WORKSPACE_ID, ADMIN_ID, "ADMIN");
        WorkspaceMember otherAdmin =
                new WorkspaceMember(WORKSPACE_ID, MEMBER_ID, "ADMIN");

        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, ADMIN_ID))
                .thenReturn(Optional.of(admin));
        when(workspaceMemberRepository
                .findByWorkspaceIdAndUserId(WORKSPACE_ID, MEMBER_ID))
                .thenReturn(Optional.of(otherAdmin));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workspaceService.removeWorkspaceMember(
                        WORKSPACE_ID, MEMBER_ID, ADMIN_EMAIL));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(workspaceMemberRepository, never()).delete(any());
    }
}