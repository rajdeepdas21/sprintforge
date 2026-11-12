
package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateSprintRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Sprint name is required")
    @Size(max = 120, message = "Sprint name cannot exceed 120 characters")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    public CreateSprintRequest() {
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
