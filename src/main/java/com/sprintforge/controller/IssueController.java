
package com.sprintforge.controller;

import com.sprintforge.dto.*;
import com.sprintforge.entity.Issue;
import com.sprintforge.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Issue createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal String email) {
        return issueService.createIssue(request, email);
    }

    @GetMapping("/{id}")
    public Issue getIssueById(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return issueService.getIssueById(id, email);
    }

    @GetMapping(params = "projectId")
    public List<Issue> getIssuesByProjectId(
            @RequestParam Long projectId,
            @AuthenticationPrincipal String email) {
        return issueService.getIssuesByProjectId(projectId, email);
    }

    @PatchMapping("/{id}/status")
    public Issue updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusRequest request,
            @AuthenticationPrincipal String email) {
        return issueService.updateIssueStatus(id, request.getStatus(), email);
    }

    @PatchMapping("/{id}/sprint")
    public Issue assignIssueToSprint(
            @PathVariable Long id,
            @Valid @RequestBody AssignIssueToSprintRequest request,
            @AuthenticationPrincipal String email) {
        return issueService.assignIssueToSprint(id, request.getSprintId(), email);
    }

    @GetMapping(params = "sprintId")
    public List<Issue> getIssuesBySprintId(
            @RequestParam Long sprintId,
            @AuthenticationPrincipal String email) {
        return issueService.getIssuesBySprintId(sprintId, email);
    }

    @PatchMapping("/{id}")
    public Issue updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal String email) {
        return issueService.updateIssue(id, request, email);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        issueService.deleteIssue(id, email);
    }
}
