package com.sprintforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateWorkspaceMemberRoleRequest(
        @NotBlank
        @Pattern(
                regexp = "ADMIN|MEMBER|VIEWER",
                message = "Role must be ADMIN, MEMBER, or VIEWER"
        )
        String role
) {}