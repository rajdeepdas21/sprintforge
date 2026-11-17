package com.sprintforge.service;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import com.sprintforge.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.UpdateProjectRequest;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.SprintRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserService userService;
    private final SprintRepository sprintRepository;
private final IssueRepository issueRepository;

    public ProjectService(
        ProjectRepository projectRepository,
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        SprintRepository sprintRepository,
        IssueRepository issueRepository,
        UserService userService) {
    this.projectRepository = projectRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.sprintRepository = sprintRepository;
    this.issueRepository = issueRepository;
    this.userService = userService;
}

    public Project createProject(CreateProjectRequest request, String email) {
        Long userId = userService.getUserIdByEmail(email);
        Long workspaceId = request.getWorkspaceId();

        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Workspace not found");
        }

        requireWorkspaceAdmin(workspaceId, userId);

        if (projectRepository.existsByWorkspaceIdAndProjectKey(
                workspaceId, request.getProjectKey())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Project key already exists in this workspace");
        }

        Project project = new Project();
        project.setWorkspaceId(workspaceId);
        project.setName(request.getName());
        project.setProjectKey(request.getProjectKey());

        return projectRepository.save(project);
    }

    public Project getProjectById(Long id, String email) {
        Long userId = userService.getUserIdByEmail(email);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"));

        if (!projectRepository.isAccessibleByUser(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this project");
        }

        return project;
    }

    public List<Project> getAllProjects(String email) {
        Long userId = userService.getUserIdByEmail(email);
        return projectRepository.findAllByUserId(userId);
    }

    private void requireWorkspaceAdmin(Long workspaceId, Long userId) {
        String role = workspaceMemberRepository
                .findRole(workspaceId, userId)
                .orElse(null);

        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can manage projects");
        }
    }

    // Only the workspace OWNER or ADMIN can edit a project.
@Transactional
public Project updateProject(
        Long projectId, UpdateProjectRequest request, String email) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found"));

    Long userId = userService.getUserIdByEmail(email);
    requireWorkspaceAdmin(project.getWorkspaceId(), userId);

    // Keep project keys unique within their workspace.
    if (projectRepository.existsByWorkspaceIdAndProjectKeyAndIdNot(
            project.getWorkspaceId(),
            request.getProjectKey(),
            projectId)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Project key already exists in this workspace");
    }

    project.setName(request.getName());
    project.setProjectKey(request.getProjectKey());

    return projectRepository.save(project);
}

// Refuse deletion while child records exist, so no data is lost.
@Transactional
public void deleteProject(Long projectId, String email) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found"));

    Long userId = userService.getUserIdByEmail(email);
    requireWorkspaceAdmin(project.getWorkspaceId(), userId);

    if (sprintRepository.existsByProjectId(projectId)
            || issueRepository.existsByProjectId(projectId)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delete or move this project's sprints and issues first");
    }

    projectRepository.delete(project);
}
}