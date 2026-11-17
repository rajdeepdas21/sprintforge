package com.sprintforge.service;

import com.sprintforge.dto.CreateSprintRequest;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.UpdateSprintRequest;
import com.sprintforge.repository.IssueRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class SprintService {

    private static final Set<String> VALID_STATUSES =
            Set.of("PLANNED", "ACTIVE", "COMPLETED");

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserService userService;
    private final IssueRepository issueRepository;

    public SprintService(
        SprintRepository sprintRepository,
        ProjectRepository projectRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        IssueRepository issueRepository,
        UserService userService) {
    this.sprintRepository = sprintRepository;
    this.projectRepository = projectRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.issueRepository = issueRepository;
    this.userService = userService;
}

    public Sprint createSprint(CreateSprintRequest request, String email) {
        Long projectId = request.getProjectId();

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        Long userId = userService.getUserIdByEmail(email);
        requireProjectAdmin(projectId, userId);

        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint start date cannot be after end date");
        }

        Sprint sprint = new Sprint();
        sprint.setProjectId(projectId);
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        return sprintRepository.save(sprint);
    }

    public Sprint getSprintById(Long id, String email) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        Long userId = userService.getUserIdByEmail(email);
        requireProjectAccess(sprint.getProjectId(), userId);

        return sprint;
    }

    public List<Sprint> getSprintsByProjectId(Long projectId, String email) {
        Long userId = userService.getUserIdByEmail(email);
        requireProjectAccess(projectId, userId);

        return sprintRepository.findByProjectId(projectId);
    }

    public Sprint updateSprintStatus(
            Long sprintId, String newStatus, String email) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        Long userId = userService.getUserIdByEmail(email);
        requireProjectAdmin(sprint.getProjectId(), userId);

        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid sprint status");
        }

        String currentStatus = sprint.getStatus();

        if (currentStatus.equals(newStatus)) {
            return sprint;
        }

        boolean validTransition =
                (currentStatus.equals("PLANNED")
                        && newStatus.equals("ACTIVE"))
                || (currentStatus.equals("ACTIVE")
                        && newStatus.equals("COMPLETED"));

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sprint status transition");
        }

        sprint.setStatus(newStatus);
        return sprintRepository.save(sprint);
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        if (!projectRepository.isAccessibleByUser(projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void requireProjectAdmin(Long projectId, Long userId) {
        Long workspaceId = projectRepository
                .findWorkspaceIdByProjectId(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        String role = workspaceMemberRepository
                .findRole(workspaceId, userId)
                .orElse(null);

        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can manage sprints");
        }
    }

    // OWNER or ADMIN can edit sprint name and dates.
// Status changes still use the existing transition-checking method.
@Transactional
public Sprint updateSprint(
        Long sprintId, UpdateSprintRequest request, String email) {
    Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sprint not found"));

    Long userId = userService.getUserIdByEmail(email);
    requireProjectAdmin(sprint.getProjectId(), userId);

    if (request.getStartDate() != null
            && request.getEndDate() != null
            && request.getStartDate().isAfter(request.getEndDate())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Sprint start date cannot be after end date");
    }

    sprint.setName(request.getName());
    sprint.setStartDate(request.getStartDate());
    sprint.setEndDate(request.getEndDate());

    return sprintRepository.save(sprint);
}

// Do not delete a sprint while issues still reference it.
@Transactional
public void deleteSprint(Long sprintId, String email) {
    Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sprint not found"));

    Long userId = userService.getUserIdByEmail(email);
    requireProjectAdmin(sprint.getProjectId(), userId);

    if (issueRepository.existsBySprintId(sprintId)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unassign this sprint's issues before deleting it");
    }

    sprintRepository.delete(sprint);
}
}