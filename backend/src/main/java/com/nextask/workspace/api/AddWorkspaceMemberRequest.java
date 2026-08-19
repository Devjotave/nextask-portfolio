package com.nextask.workspace.api;

import com.nextask.workspace.entity.WorkspaceRole;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWorkspaceMemberRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotNull(message = "role is required")
        WorkspaceRole role) {
}
