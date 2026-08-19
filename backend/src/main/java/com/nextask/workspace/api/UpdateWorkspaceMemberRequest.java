package com.nextask.workspace.api;

import com.nextask.workspace.entity.WorkspaceRole;

import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceMemberRequest(
        @NotNull(message = "role is required")
        WorkspaceRole role) {
}
