
package com.sprintforge.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "projects",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_project_key_per_workspace",
            columnNames = {"workspace_id", "project_key"}
        )
    }
)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String projectKey;

    public Project() {
    }

    public Long getId() {
        return id;
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
