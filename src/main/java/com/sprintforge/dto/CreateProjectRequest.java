
package com.sprintforge.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest {

    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    @NotBlank(message = "Project name is required")
    @Size(max = 120, message = "Project name cannot exceed 120 characters")
    private String name;

    @NotBlank(message = "Project key is required")
@Size(min = 2, max = 10,
      message = "Project key must be between 2 and 10 characters")
@Pattern(regexp = "^[A-Z0-9]+$",
         message = "Project key must contain only uppercase letters and digits")
private String projectKey;

    public CreateProjectRequest() {
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

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
