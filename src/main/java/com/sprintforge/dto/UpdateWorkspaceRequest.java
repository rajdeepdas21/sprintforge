package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A workspace update currently changes only its name.
public class UpdateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 120, message = "Workspace name cannot exceed 120 characters")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}