package com.sprintforge.entity;

import java.io.Serializable;
import java.util.Objects;

public class WorkspaceMemberId implements Serializable {

    private Long workspaceId;
    private Long userId;

    public WorkspaceMemberId() {}

    public WorkspaceMemberId(Long workspaceId, Long userId) {
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMemberId that)) return false;
        return Objects.equals(workspaceId, that.workspaceId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, userId);
    }
}