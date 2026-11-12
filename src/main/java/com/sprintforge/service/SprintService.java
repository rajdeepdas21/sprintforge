
package com.sprintforge.service;

import com.sprintforge.dto.CreateSprintRequest;
import com.sprintforge.entity.Sprint;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

//import com.sprintforge.service.UserService;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    public SprintService(
        SprintRepository sprintRepository,
        ProjectRepository projectRepository,
        UserService userService) {

    this.sprintRepository = sprintRepository;
    this.projectRepository = projectRepository;
    this.userService = userService;
}

    public Sprint createSprint(CreateSprintRequest request, String email) {

        // Check whether the project exists
        if (!projectRepository.existsById(request.getProjectId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            );
        }

        Long userId = userService.getUserIdByEmail(email);

if (!projectRepository.isOwnedByUser(
        request.getProjectId(), userId)) {

    throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Access denied"
    );
}

        // Validate sprint dates
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint start date cannot be after end date"
            );
        }

        // Create the Sprint entity
        Sprint sprint = new Sprint();

        sprint.setProjectId(request.getProjectId());
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        // Status defaults to PLANNED in the entity

        return sprintRepository.save(sprint);
    }

public Sprint getSprintById(Long id, String email) {

    Long userId = userService.getUserIdByEmail(email);

    Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sprint not found"
            ));

    if (!projectRepository.isOwnedByUser(
            sprint.getProjectId(), userId)) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied"
        );
    }

    return sprint;
}

public List<Sprint> getSprintsByProjectId(Long projectId, String email) {

    Long userId = userService.getUserIdByEmail(email);

    if (!projectRepository.existsById(projectId)) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Project not found");
    }

    if (!projectRepository.isOwnedByUser(projectId, userId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied");
    }

    return sprintRepository.findByProjectId(projectId);
}

public Sprint updateSprintStatus(Long sprintId, String newStatus, String email) {

    Sprint sprint = sprintRepository.findById(sprintId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Sprint not found"));

    Long userId = userService.getUserIdByEmail(email);

    if (!projectRepository.isOwnedByUser(sprint.getProjectId(), userId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied");
    }

    Set<String> validStatuses = Set.of("PLANNED", "ACTIVE", "COMPLETED");

    if (!validStatuses.contains(newStatus)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid sprint status");
    }

    String currentStatus = sprint.getStatus();

    if (currentStatus.equals(newStatus)) {
        return sprint;
    }

    boolean validTransition =
        (currentStatus.equals("PLANNED") && newStatus.equals("ACTIVE"))
        || (currentStatus.equals("ACTIVE") && newStatus.equals("COMPLETED"));

    if (!validTransition) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid sprint status transition");
    }

    sprint.setStatus(newStatus);

    return sprintRepository.save(sprint);
}

}
