
package com.sprintforge.controller;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Project createProject(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal String email) {

    return projectService.createProject(request, email);
}
    
@GetMapping("/{id}")
public Project getProjectById(
        @PathVariable Long id,
        @AuthenticationPrincipal String email
) {
    return projectService.getProjectById(id, email);
}

    
@GetMapping
public List<Project> getAllProjects(
        @AuthenticationPrincipal String email
) {
    return projectService.getAllProjects(email);
}



}
