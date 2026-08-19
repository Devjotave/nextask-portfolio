package com.nextask.user.api;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
