package com.nextask.workspace;

import org.springframework.stereotype.Component;

import com.nextask.workspace.entity.WorkspaceMemberId;
import com.nextask.workspace.error.UserMustBelongToWorkspaceException;

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
}
