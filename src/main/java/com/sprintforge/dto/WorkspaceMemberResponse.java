package com.sprintforge.dto;

import java.time.OffsetDateTime;

public record WorkspaceMemberResponse(
        Long userId,
        String name,
        String email,
        String role,
        OffsetDateTime joinedAt
) {}