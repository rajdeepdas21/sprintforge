
package com.sprintforge.dto;

import jakarta.validation.constraints.NotNull;

public class AssignIssueToSprintRequest {

    @NotNull(message = "Sprint ID is required")
    private Long sprintId;

    public AssignIssueToSprintRequest() {
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }
}
