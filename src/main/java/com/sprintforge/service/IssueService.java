package com.sprintforge.service;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.dto.UpdateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.IssueSpecification;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.repository.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.AssignIssueToUserRequest;
import java.util.List;
import java.util.Set;

@Service
public class IssueService {

    private static final Set<String> VALID_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final Set<String> VALID_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserService userService;

    public IssueService(
            IssueRepository issueRepository,
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserService userService) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userService = userService;
    }

    private void verifyProjectAccess(Long projectId, String email) {
        Long userId = userService.getUserIdByEmail(email);

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        if (!projectRepository.isAccessibleByUser(projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void verifyIssueWriteAccess(Long projectId, String email) {
        Long userId = userService.getUserIdByEmail(email);

        Long workspaceId = projectRepository
                .findWorkspaceIdByProjectId(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        String role = workspaceMemberRepository
                .findRole(workspaceId, userId)
                .orElse(null);

        if (!"OWNER".equals(role)
                && !"ADMIN".equals(role)
                && !"MEMBER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to change issues");
        }
    }

    private Issue getAuthorizedIssue(Long issueId, String email) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));

        verifyProjectAccess(issue.getProjectId(), email);
        return issue;
    }

    private Issue getWritableIssue(Long issueId, String email) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));

        verifyIssueWriteAccess(issue.getProjectId(), email);
        return issue;
    }

    public Issue createIssue(CreateIssueRequest request, String email) {
        String priority = request.getPriority();

        if (priority != null && !VALID_PRIORITIES.contains(priority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Priority must be LOW, MEDIUM, HIGH, or URGENT");
        }

        verifyIssueWriteAccess(request.getProjectId(), email);

        Issue issue = new Issue();
        issue.setProjectId(request.getProjectId());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());

        if (priority != null) {
            issue.setPriority(priority);
        }

        return issueRepository.save(issue);
    }

    public Issue getIssueById(Long id, String email) {
        return getAuthorizedIssue(id, email);
    }

    public Page<Issue> getIssuesByProjectId(
            Long projectId,
            String email,
            String status,
            String priority,
            Long sprintId,
            String title,
            Pageable pageable) {

        verifyProjectAccess(projectId, email);

        Specification<Issue> spec =
                IssueSpecification.hasProjectId(projectId);

        if (status != null && !status.isBlank()) {
            if (!VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid issue status");
            }
            spec = spec.and(IssueSpecification.hasStatus(status));
        }

        if (priority != null && !priority.isBlank()) {
            if (!VALID_PRIORITIES.contains(priority)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid issue priority");
            }
            spec = spec.and(IssueSpecification.hasPriority(priority));
        }

        if (sprintId != null) {
            spec = spec.and(IssueSpecification.hasSprintId(sprintId));
        }

        if (title != null && !title.isBlank()) {
            spec = spec.and(
                    IssueSpecification.titleContains(title.trim()));
        }

        return issueRepository.findAll(spec, pageable);
    }

    public Issue updateIssueStatus(Long id, String status, String email) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be TODO, IN_PROGRESS, IN_REVIEW, or DONE");
        }

        Issue issue = getWritableIssue(id, email);
        issue.setStatus(status);
        return issueRepository.save(issue);
    }

    public Issue assignIssueToSprint(
            Long issueId, Long sprintId, String email) {
        Issue issue = getWritableIssue(issueId, email);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        verifyProjectAccess(sprint.getProjectId(), email);

        if (!issue.getProjectId().equals(sprint.getProjectId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Issue and sprint must belong to the same project");
        }

        issue.setSprintId(sprintId);
        return issueRepository.save(issue);
    }

    public List<Issue> getIssuesBySprintId(Long sprintId, String email) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        verifyProjectAccess(sprint.getProjectId(), email);
        return issueRepository.findBySprintId(sprintId);
    }

    public Issue updateIssue(
            Long issueId, UpdateIssueRequest request, String email) {
        Issue issue = getWritableIssue(issueId, email);

        String priority = request.getPriority();

        if (priority != null && !VALID_PRIORITIES.contains(priority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Priority must be LOW, MEDIUM, HIGH, or URGENT");
        }

        issue.setTitle(request.getTitle());

        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }

        if (priority != null) {
            issue.setPriority(priority);
        }

        return issueRepository.save(issue);
    }

    public void deleteIssue(Long issueId, String email) {
        Issue issue = getWritableIssue(issueId, email);
        issueRepository.delete(issue);
    }

   public Issue assignIssueToUser(
        Long issueId,
        AssignIssueToUserRequest request,
        String requesterEmail) {

    // Allows OWNER, ADMIN, or MEMBER; rejects VIEWER.
    Issue issue = getWritableIssue(issueId, requesterEmail);

    Long requesterId = userService.getUserIdByEmail(requesterEmail);

    Long workspaceId = projectRepository
            .findWorkspaceIdByProjectId(issue.getProjectId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found"));

    Long assigneeId = userService
            .getUserByEmail(request.email())
            .getId();

    if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
            workspaceId, assigneeId)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Assignee must be a member of this workspace");
    }

    if (requesterId.equals(assigneeId)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Self-assignment is not allowed");
    }

    issue.setAssigneeId(assigneeId);
    return issueRepository.save(issue);
}

public Issue unassignIssue(
        Long issueId,
        String requesterEmail) {

    Issue issue = getWritableIssue(issueId, requesterEmail);
    issue.setAssigneeId(null);
    return issueRepository.save(issue);
}
// Remove an issue from its sprint while keeping the issue itself.
public Issue unassignIssueFromSprint(Long issueId, String email) {
    // Reuse the existing write permission check; VIEWERs cannot modify issues.
    Issue issue = getWritableIssue(issueId, email);

    // Make this operation safe to repeat when the issue has no sprint.
    if (issue.getSprintId() == null) {
        return issue;
    }

    issue.setSprintId(null);
    return issueRepository.save(issue);
}
}