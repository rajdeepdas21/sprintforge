
package com.sprintforge.controller;

import com.sprintforge.dto.*;
import com.sprintforge.entity.Issue;
import com.sprintforge.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Set;
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
public Page<Issue> getIssuesByProjectId(
        @RequestParam Long projectId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "DESC") String direction,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) Long sprintId,
        @RequestParam(required = false) String title,
        @AuthenticationPrincipal String email) {

    // Validate pagination
    if (page < 0 || size < 1 || size > 100) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page must be >= 0 and size must be between 1 and 100");
    }

    // Prevent arbitrary sorting by invalid fields
    Set<String> allowedSortFields =
            Set.of("id", "title", "status", "priority", "sprintId");

    if (!allowedSortFields.contains(sortBy)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid sort field");
    }

    Sort.Direction sortDirection;

    try {
        sortDirection = Sort.Direction.valueOf(
                direction.toUpperCase());
    } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Direction must be ASC or DESC");
    }

    Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(sortDirection, sortBy));

    return issueService.getIssuesByProjectId(
            projectId,
            email,
            status,
            priority,
            sprintId,
            title,
            pageable);
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

    @PatchMapping("/{id}/assignee")
public Issue assignIssueToUser(
        @PathVariable Long id,
        @Valid @RequestBody AssignIssueToUserRequest request,
        @AuthenticationPrincipal String email) {
    return issueService.assignIssueToUser(id, request, email);
}

@DeleteMapping("/{id}/assignee")
public Issue unassignIssue(
        @PathVariable Long id,
        @AuthenticationPrincipal String email) {
    return issueService.unassignIssue(id, email);
}
}
