package com.nextask.workspace.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WorkspaceMemberId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected WorkspaceMemberId() {
        // Required by JPA.
    }

    public WorkspaceMemberId(Long workspaceId, Long userId) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceMemberId that)) {
            return false;
        }
        return Objects.equals(workspaceId, that.workspaceId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, userId);
    }
}
