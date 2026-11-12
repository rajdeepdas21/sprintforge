
package com.sprintforge.controller;

import com.sprintforge.dto.CreateWorkspaceRequest;
import com.sprintforge.entity.Workspace;
import com.sprintforge.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

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
}
