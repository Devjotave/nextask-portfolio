package com.nextask.workspace.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import com.nextask.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_members", schema = "dbo")
public class WorkspaceMember {

    @EmbeddedId
    private WorkspaceMemberId id;

    @MapsId("workspaceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected WorkspaceMember() {
        // Required by JPA.
    }

    public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        this.workspace = Objects.requireNonNull(workspace, "workspace is required");
        this.user = Objects.requireNonNull(user, "user is required");
        this.id = new WorkspaceMemberId(workspace.getId(), user.getId());
        changeRole(role);
    }

    public void changeRole(WorkspaceRole role) {
        this.role = Objects.requireNonNull(role, "role is required");
    }

    @PrePersist
    private void beforeInsert() {
        joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public WorkspaceMemberId getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getUser() {
        return user;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
