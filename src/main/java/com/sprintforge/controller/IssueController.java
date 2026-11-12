
package com.sprintforge.controller;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.service.IssueService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.sprintforge.dto.UpdateIssueStatusRequest;
import com.sprintforge.dto.AssignIssueToSprintRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import com.sprintforge.dto.UpdateIssueRequest;
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
            @Valid @RequestBody CreateIssueRequest request) {
        return issueService.createIssue(request);
    }

    @GetMapping("/{id}")
    public Issue getIssueById(@PathVariable Long id) {
        return issueService.getIssueById(id);
    }

    @GetMapping
    public java.util.List<Issue> getIssuesByProjectId(
            @RequestParam Long projectId) {
        return issueService.getIssuesByProjectId(projectId);
    }

    @PatchMapping("/{id}/status")
public Issue updateIssueStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIssueStatusRequest request) {

    return issueService.updateIssueStatus(id, request.getStatus());

    
}
@PatchMapping("/{id}/sprint")
public Issue assignIssueToSprint(
        @PathVariable Long id,
        @Valid @RequestBody AssignIssueToSprintRequest request) {

    return issueService.assignIssueToSprint(
            id,
            request.getSprintId()
    );
}

@GetMapping(params = "sprintId")
public List<Issue> getIssuesBySprintId(
        @RequestParam Long sprintId) {

    return issueService.getIssuesBySprintId(sprintId);
}

@PatchMapping("/{id}")
public Issue updateIssue(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIssueRequest request) {

    return issueService.updateIssue(id, request);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteIssue(@PathVariable Long id) {
    issueService.deleteIssue(id);
}
}
