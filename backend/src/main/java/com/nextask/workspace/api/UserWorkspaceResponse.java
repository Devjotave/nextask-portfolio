package com.nextask.workspace.api;

import java.time.OffsetDateTime;

import com.nextask.workspace.entity.WorkspaceRole;

public record UserWorkspaceResponse(
        Long id,
        String name,
        WorkspaceRole role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
