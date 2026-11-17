package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PUT replaces the editable project name and key.
public class UpdateProjectRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[A-Z0-9]+$")
    private String projectKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }
}