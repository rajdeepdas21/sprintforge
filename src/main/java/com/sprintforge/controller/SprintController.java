
package com.sprintforge.controller;

import com.sprintforge.dto.CreateSprintRequest;
import com.sprintforge.entity.Sprint;
import com.sprintforge.service.SprintService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.sprintforge.dto.UpdateSprintStatusRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Sprint createSprint(
        @Valid @RequestBody CreateSprintRequest request,
        @AuthenticationPrincipal String email) {

    return sprintService.createSprint(request, email);
}

    @GetMapping("/{id}")
public Sprint getSprintById(@PathVariable Long id,
    @AuthenticationPrincipal String email) {
    return sprintService.getSprintById(id,email);
}

@GetMapping
public List<Sprint> getSprintsByProjectId(
        @RequestParam Long projectId,
        @AuthenticationPrincipal String email) {
    return sprintService.getSprintsByProjectId(projectId, email);
}
@PatchMapping("/{id}/status")
public Sprint updateSprintStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSprintStatusRequest request,
    @AuthenticationPrincipal String email) {

    return sprintService.updateSprintStatus(
            id,
            request.getStatus(),
            email
    );
}
}
