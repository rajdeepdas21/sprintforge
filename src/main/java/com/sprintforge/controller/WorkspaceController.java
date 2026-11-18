
package com.sprintforge.controller;

import com.sprintforge.dto.AddWorkspaceMemberRequest;
import com.sprintforge.dto.CreateWorkspaceRequest;
import com.sprintforge.dto.WorkspaceMemberResponse;
import com.sprintforge.entity.Workspace;
import com.sprintforge.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
import com.sprintforge.dto.UpdateWorkspaceMemberRoleRequest;
import com.sprintforge.entity.WorkspaceMember;
import com.sprintforge.dto.UpdateWorkspaceRequest;
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Workspace createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal String email
    ) {
        return workspaceService.createWorkspace(request, email);
    }

    @GetMapping
public List<Workspace> getAllWorkspaces(
        @AuthenticationPrincipal String email) {
    return workspaceService.getAllWorkspaces(email);
}

@GetMapping("/{id}")
public Workspace getWorkspaceById(
        @PathVariable Long id,
        @AuthenticationPrincipal String email) {
    return workspaceService.getWorkspaceById(id, email);
}
@GetMapping("/{workspaceId}/members")
public ResponseEntity<List<WorkspaceMemberResponse>> getWorkspaceMembers(
        @PathVariable Long workspaceId,
        Authentication authentication) {

    return ResponseEntity.ok(
            workspaceService.getWorkspaceMembers(
                    workspaceId,
                    authentication.getName()
            )
    );
}

@PostMapping("/{workspaceId}/members")
public ResponseEntity<WorkspaceMemberResponse> addWorkspaceMember(
        @PathVariable Long workspaceId,
        @Valid @RequestBody AddWorkspaceMemberRequest request,
        Authentication authentication) {

    return ResponseEntity.status(HttpStatus.CREATED).body(
            workspaceService.addWorkspaceMember(
                    workspaceId,
                    request,
                    authentication.getName()
            )
    );
}

@PatchMapping("/{workspaceId}/members/{userId}/role")
public WorkspaceMember updateWorkspaceMemberRole(
        @PathVariable Long workspaceId,
        @PathVariable Long userId,
        @Valid @RequestBody UpdateWorkspaceMemberRoleRequest request,
        Authentication authentication) {

    return workspaceService.updateWorkspaceMemberRole(
            workspaceId,
            userId,
            request,
            authentication.getName());
}

@DeleteMapping("/{workspaceId}/members/{userId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void removeWorkspaceMember(
        @PathVariable Long workspaceId,
        @PathVariable Long userId,
        Authentication authentication) {

    workspaceService.removeWorkspaceMember(
            workspaceId,
            userId,
            authentication.getName());
}
// Rename a workspace; the service checks that the requester is its owner.
@PutMapping("/{id}")
public Workspace updateWorkspace(
        @PathVariable Long id,
        @Valid @RequestBody UpdateWorkspaceRequest request,
        Authentication authentication) {
    return workspaceService.updateWorkspace(
            id, request, authentication.getName());
}

// Delete an empty workspace; returns HTTP 204 when successful.
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteWorkspace(
        @PathVariable Long id,
        Authentication authentication) {
    workspaceService.deleteWorkspace(id, authentication.getName());
}
}
