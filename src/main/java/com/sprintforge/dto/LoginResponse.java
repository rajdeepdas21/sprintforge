
package com.sprintforge.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}
