
package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateSprintStatusRequest {

    @NotBlank(message = "Sprint status is required")
    private String status;

    public UpdateSprintStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
