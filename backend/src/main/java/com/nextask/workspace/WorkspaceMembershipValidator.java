package com.nextask.workspace;

import org.springframework.stereotype.Component;

import com.nextask.workspace.entity.WorkspaceMemberId;
import com.nextask.workspace.entity.WorkspaceRole;
import com.nextask.workspace.error.UserMustBelongToWorkspaceException;
import com.nextask.workspace.error.WorkspaceAccessDeniedException;

@Component
public class WorkspaceMembershipValidator {

    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceMembershipValidator(WorkspaceMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void requireMember(Long workspaceId, Long userId) {
        if (!memberRepository.existsById(new WorkspaceMemberId(workspaceId, userId))) {
            throw new UserMustBelongToWorkspaceException(workspaceId, userId);
        }
    }

    public void requireAccess(Long workspaceId, Long userId) {
        if (!memberRepository.existsById(new WorkspaceMemberId(workspaceId, userId))) {
            throw new WorkspaceAccessDeniedException(workspaceId);
        }
    }

    public void requireOwner(Long workspaceId, Long userId) {
        WorkspaceMemberId memberId = new WorkspaceMemberId(workspaceId, userId);
        if (!memberRepository.existsByIdAndRole(memberId, WorkspaceRole.OWNER)) {
            throw new WorkspaceAccessDeniedException(workspaceId);
        }
    }

    public void requireSelfOrOwner(Long workspaceId, Long currentUserId, Long targetUserId) {
        requireAccess(workspaceId, currentUserId);
        if (!currentUserId.equals(targetUserId)) {
            requireOwner(workspaceId, currentUserId);
        }
    }
}
