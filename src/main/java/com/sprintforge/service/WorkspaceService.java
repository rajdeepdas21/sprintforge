
package com.sprintforge.service;

import com.sprintforge.dto.CreateWorkspaceRequest;
import com.sprintforge.entity.Workspace;
import com.sprintforge.repository.WorkspaceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserService userService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userService = userService;
    }

    public Workspace createWorkspace(
            CreateWorkspaceRequest request,
            String email
    ) {

        Long userId = userService.getUserIdByEmail(email);

        Workspace workspace = new Workspace();

        workspace.setName(request.getName());
        workspace.setOwnerId(userId);

        return workspaceRepository.save(workspace);
    }
    public List<Workspace> getAllWorkspaces(String email) {
    Long userId = userService.getUserIdByEmail(email);
    return workspaceRepository.findAllByOwnerId(userId);
}

public Workspace getWorkspaceById(Long id, String email) {
    Long userId = userService.getUserIdByEmail(email);

    Workspace workspace = workspaceRepository.findById(id)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Workspace not found"));

    if (!workspaceRepository.existsByIdAndOwnerId(id, userId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Access denied");
    }

    return workspace;
}


}
