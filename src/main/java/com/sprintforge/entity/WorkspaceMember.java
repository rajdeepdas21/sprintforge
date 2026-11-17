package com.sprintforge.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "workspace_members")
@IdClass(WorkspaceMemberId.class)
public class WorkspaceMember {

    @Id
    @Column(name = "workspace_id")
    private Long workspaceId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    public WorkspaceMember() {}

    public WorkspaceMember(Long workspaceId, Long userId, String role) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = OffsetDateTime.now();
    }

    public Long getWorkspaceId() { return workspaceId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }

    public void setRole(String role) { this.role = role; }
}