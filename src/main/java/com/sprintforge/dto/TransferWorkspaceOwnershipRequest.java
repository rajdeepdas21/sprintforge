package com.sprintforge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// The new owner must be an existing user ID greater than zero.
public record TransferWorkspaceOwnershipRequest(
        @NotNull(message = "New owner ID is required")
        @Positive(message = "New owner ID must be positive")
        Long newOwnerId
) {}