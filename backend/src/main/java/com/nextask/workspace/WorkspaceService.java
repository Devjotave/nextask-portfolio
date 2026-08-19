package com.nextask.workspace;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.api.PageResponse;
import com.nextask.entity.User;
import com.nextask.user.UserRepository;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.api.UpdateWorkspaceMemberRequest;
import com.nextask.workspace.api.UpdateWorkspaceRequest;
import com.nextask.workspace.api.UserWorkspaceResponse;
import com.nextask.workspace.api.WorkspaceMemberResponse;
import com.nextask.workspace.api.WorkspaceResponse;
import com.nextask.workspace.entity.Workspace;
import com.nextask.workspace.entity.WorkspaceMember;
import com.nextask.workspace.entity.WorkspaceMemberId;
import com.nextask.workspace.entity.WorkspaceRole;
import com.nextask.workspace.error.WorkspaceMemberAlreadyExistsException;
import com.nextask.workspace.error.WorkspaceMemberNotFoundException;
import com.nextask.workspace.error.WorkspaceMustHaveOwnerException;
import com.nextask.workspace.error.WorkspaceNotFoundException;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        User owner = findUser(request.ownerId());
        Workspace workspace = workspaceRepository.saveAndFlush(new Workspace(request.name()));
        memberRepository.saveAndFlush(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
        return toWorkspaceResponse(workspace);
    }

    public WorkspaceResponse findById(Long workspaceId) {
        return toWorkspaceResponse(findWorkspace(workspaceId));
    }

    public PageResponse<UserWorkspaceResponse> findAllForUser(
            Long userId,
            int page,
            int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Page<UserWorkspaceResponse> workspaces = memberRepository
                .findByUser_IdOrderByWorkspace_IdAsc(userId, PageRequest.of(page, size))
                .map(WorkspaceService::toUserWorkspaceResponse);
        return PageResponse.from(workspaces);
    }

    @Transactional
    public WorkspaceResponse update(Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = findWorkspace(workspaceId);
        workspace.rename(request.name());
        return toWorkspaceResponse(workspaceRepository.saveAndFlush(workspace));
    }

    @Transactional
    public void delete(Long workspaceId) {
        Workspace workspace = findWorkspaceForUpdate(workspaceId);
        workspaceRepository.delete(workspace);
        workspaceRepository.flush();
    }

    public PageResponse<WorkspaceMemberResponse> findMembers(
            Long workspaceId,
            int page,
            int size) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException(workspaceId);
        }

        Page<WorkspaceMemberResponse> members = memberRepository
                .findByWorkspace_IdOrderByUser_IdAsc(workspaceId, PageRequest.of(page, size))
                .map(WorkspaceService::toMemberResponse);
        return PageResponse.from(members);
    }

    @Transactional
    public WorkspaceMemberResponse addMember(
            Long workspaceId,
            AddWorkspaceMemberRequest request) {
        Workspace workspace = findWorkspace(workspaceId);
        User user = findUser(request.userId());
        WorkspaceMemberId memberId = new WorkspaceMemberId(workspaceId, request.userId());

        if (memberRepository.existsById(memberId)) {
            throw new WorkspaceMemberAlreadyExistsException(workspaceId, request.userId());
        }

        try {
            WorkspaceMember member = new WorkspaceMember(workspace, user, request.role());
            return toMemberResponse(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            throw new WorkspaceMemberAlreadyExistsException(workspaceId, request.userId());
        }
    }

    @Transactional
    public WorkspaceMemberResponse updateMember(
            Long workspaceId,
            Long userId,
            UpdateWorkspaceMemberRequest request) {
        findWorkspaceForUpdate(workspaceId);
        WorkspaceMember member = findMember(workspaceId, userId);

        if (member.getRole() == WorkspaceRole.OWNER
                && request.role() == WorkspaceRole.MEMBER) {
            requireAnotherOwner(workspaceId);
        }

        member.changeRole(request.role());
        return toMemberResponse(memberRepository.saveAndFlush(member));
    }

    @Transactional
    public void removeMember(Long workspaceId, Long userId) {
        findWorkspaceForUpdate(workspaceId);
        WorkspaceMember member = findMember(workspaceId, userId);

        if (member.getRole() == WorkspaceRole.OWNER) {
            requireAnotherOwner(workspaceId);
        }

        memberRepository.delete(member);
        memberRepository.flush();
    }

    private void requireAnotherOwner(Long workspaceId) {
        long ownerCount = memberRepository.countByWorkspace_IdAndRole(
                workspaceId,
                WorkspaceRole.OWNER);
        if (ownerCount <= 1) {
            throw new WorkspaceMustHaveOwnerException(workspaceId);
        }
    }

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
    }

    private Workspace findWorkspaceForUpdate(Long workspaceId) {
        return workspaceRepository.findByIdForUpdate(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
    }

    private WorkspaceMember findMember(Long workspaceId, Long userId) {
        return memberRepository.findById(new WorkspaceMemberId(workspaceId, userId))
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspaceId, userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private static WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

    private static UserWorkspaceResponse toUserWorkspaceResponse(WorkspaceMember member) {
        Workspace workspace = member.getWorkspace();
        return new UserWorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                member.getRole(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

    private static WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        User user = member.getUser();
        return new WorkspaceMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }
}
