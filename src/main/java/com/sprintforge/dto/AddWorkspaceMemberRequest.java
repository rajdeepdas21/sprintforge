package com.sprintforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddWorkspaceMemberRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(
                regexp = "ADMIN|MEMBER|VIEWER",
                message = "Role must be ADMIN, MEMBER, or VIEWER"
        )
        String role

) {}