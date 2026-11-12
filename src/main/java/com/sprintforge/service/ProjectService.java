
package com.sprintforge.service;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceRepository workspaceRepository,
            UserService userService) {

        this.projectRepository = projectRepository;
        this.workspaceRepository = workspaceRepository;
        this.userService = userService;
    }

    public Project createProject(CreateProjectRequest request, String email) {

        // Get authenticated user's database ID
        Long userId = userService.getUserIdByEmail(email);

        // Check whether workspace exists
        if (!workspaceRepository.existsById(request.getWorkspaceId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found"
            );
        }

        // Check whether workspace belongs to authenticated user
        if (!workspaceRepository.isOwnedByUser(
                request.getWorkspaceId(), userId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this workspace"
            );
        }

        // Check duplicate project key
        if (projectRepository.existsByWorkspaceIdAndProjectKey(
                request.getWorkspaceId(),
                request.getProjectKey())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Project key already exists in this workspace"
            );
        }

        // Create project
        Project project = new Project();
        project.setWorkspaceId(request.getWorkspaceId());
        project.setName(request.getName());
        project.setProjectKey(request.getProjectKey());

        return projectRepository.save(project);
    }

    
public Project getProjectById(Long id, String email) {

    Long userId = userService.getUserIdByEmail(email);

    Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            ));

    if (!workspaceRepository.isOwnedByUser(
            project.getWorkspaceId(), userId)) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have access to this project"
        );
    }

    return project;
}


    
public List<Project> getAllProjects(String email) {

    Long userId = userService.getUserIdByEmail(email);

    return projectRepository.findAllByUserId(userId);
}

}
