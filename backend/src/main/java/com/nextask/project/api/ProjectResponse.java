package com.nextask.project.api;

import java.time.OffsetDateTime;

public record ProjectResponse(
        Long id,
        Long workspaceId,
        String name,
        String description,
        Long createdById,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
