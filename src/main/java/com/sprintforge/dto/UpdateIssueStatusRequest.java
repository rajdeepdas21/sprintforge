
package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateIssueStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "TODO|IN_PROGRESS|IN_REVIEW|DONE",
        message = "Status must be TODO, IN_PROGRESS, IN_REVIEW, or DONE"
    )
    private String status;

    public UpdateIssueStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
