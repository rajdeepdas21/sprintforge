
package com.sprintforge.service;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.dto.UpdateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserService userService;

    private static final Set<String> VALID_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final Set<String> VALID_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");

    public IssueService(
            IssueRepository issueRepository,
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            UserService userService) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.userService = userService;
    }

    private void verifyProjectOwnership(Long projectId, String email) {
        Long userId = userService.getUserIdByEmail(email);

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        if (!projectRepository.isOwnedByUser(projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private Issue getAuthorizedIssue(Long issueId, String email) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));

        verifyProjectOwnership(issue.getProjectId(), email);
        return issue;
    }

    public Issue createIssue(CreateIssueRequest request, String email) {
        String priority = request.getPriority();

        if (priority != null && !VALID_PRIORITIES.contains(priority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Priority must be LOW, MEDIUM, HIGH, or URGENT");
        }

        verifyProjectOwnership(request.getProjectId(), email);

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

    public List<Issue> getIssuesByProjectId(Long projectId, String email) {
        verifyProjectOwnership(projectId, email);
        return issueRepository.findByProjectId(projectId);
    }

    public Issue updateIssueStatus(Long id, String status, String email) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be TODO, IN_PROGRESS, IN_REVIEW, or DONE");
        }

        Issue issue = getAuthorizedIssue(id, email);
        issue.setStatus(status);
        return issueRepository.save(issue);
    }

    public Issue assignIssueToSprint(
            Long issueId, Long sprintId, String email) {

        Issue issue = getAuthorizedIssue(issueId, email);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        verifyProjectOwnership(sprint.getProjectId(), email);

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

        verifyProjectOwnership(sprint.getProjectId(), email);
        return issueRepository.findBySprintId(sprintId);
    }

    public Issue updateIssue(
            Long issueId, UpdateIssueRequest request, String email) {

        Issue issue = getAuthorizedIssue(issueId, email);

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
        Issue issue = getAuthorizedIssue(issueId, email);
        issueRepository.delete(issue);
    }
}
