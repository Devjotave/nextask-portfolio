package com.nextask.workspace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nextask.workspace.entity.WorkspaceMember;
import com.nextask.workspace.entity.WorkspaceMemberId;
import com.nextask.workspace.entity.WorkspaceRole;

public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

    long countByWorkspace_IdAndRole(Long workspaceId, WorkspaceRole role);

    @EntityGraph(attributePaths = "workspace")
    Page<WorkspaceMember> findByUser_IdOrderByWorkspace_IdAsc(
            Long userId,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<WorkspaceMember> findByWorkspace_IdOrderByUser_IdAsc(
            Long workspaceId,
            Pageable pageable);
}
