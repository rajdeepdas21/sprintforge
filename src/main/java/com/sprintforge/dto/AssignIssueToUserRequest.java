package com.sprintforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AssignIssueToUserRequest(
        @NotBlank @Email String email
) {}