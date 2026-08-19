package com.nextask.workspace.api;

import java.time.OffsetDateTime;

public record WorkspaceResponse(
        Long id,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
