package com.nextask.workspace.api;

import java.time.OffsetDateTime;

import com.nextask.workspace.entity.WorkspaceRole;

public record WorkspaceMemberResponse(
        Long userId,
        String name,
        String email,
        WorkspaceRole role,
        OffsetDateTime joinedAt) {
}
