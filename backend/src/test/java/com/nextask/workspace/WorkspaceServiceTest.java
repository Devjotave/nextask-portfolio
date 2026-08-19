package com.nextask.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.nextask.entity.User;
import com.nextask.user.UserRepository;
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.api.UpdateWorkspaceMemberRequest;
import com.nextask.workspace.api.WorkspaceResponse;
import com.nextask.workspace.entity.Workspace;
import com.nextask.workspace.entity.WorkspaceMember;
import com.nextask.workspace.entity.WorkspaceMemberId;
import com.nextask.workspace.entity.WorkspaceRole;
import com.nextask.workspace.error.WorkspaceMemberAlreadyExistsException;
import com.nextask.workspace.error.WorkspaceMustHaveOwnerException;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 18, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(
                workspaceRepository,
                memberRepository,
                userRepository);
    }

    @Test
    void createsWorkspaceWithOwnerMembership() {
        User owner = user(7L, "Owner", "owner@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.saveAndFlush(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            initialize(workspace, 21L);
            return workspace;
        });
        when(memberRepository.saveAndFlush(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> {
                    WorkspaceMember member = invocation.getArgument(0);
                    ReflectionTestUtils.setField(member, "joinedAt", TIMESTAMP);
                    return member;
                });

        WorkspaceResponse response = workspaceService.create(
                new CreateWorkspaceRequest(" Product Team ", 7L));

        ArgumentCaptor<WorkspaceMember> memberCaptor =
                ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getId())
                .isEqualTo(new WorkspaceMemberId(21L, 7L));
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.name()).isEqualTo("Product Team");
    }

    @Test
    void rejectsAddingExistingMember() {
        Workspace workspace = workspace(21L, "Product Team");
        User user = user(7L, "Member", "member@example.com");
        WorkspaceMemberId id = new WorkspaceMemberId(21L, 7L);
        when(workspaceRepository.findById(21L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(memberRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.addMember(
                21L,
                new AddWorkspaceMemberRequest(7L, WorkspaceRole.MEMBER)))
                .isInstanceOf(WorkspaceMemberAlreadyExistsException.class);

        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDemotingTheOnlyOwner() {
        Workspace workspace = workspace(21L, "Product Team");
        User owner = user(7L, "Owner", "owner@example.com");
        WorkspaceMember membership = member(workspace, owner, WorkspaceRole.OWNER);
        when(workspaceRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findById(new WorkspaceMemberId(21L, 7L)))
                .thenReturn(Optional.of(membership));
        when(memberRepository.countByWorkspace_IdAndRole(21L, WorkspaceRole.OWNER))
                .thenReturn(1L);

        assertThatThrownBy(() -> workspaceService.updateMember(
                21L,
                7L,
                new UpdateWorkspaceMemberRequest(WorkspaceRole.MEMBER)))
                .isInstanceOf(WorkspaceMustHaveOwnerException.class);

        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsRemovingTheOnlyOwner() {
        Workspace workspace = workspace(21L, "Product Team");
        User owner = user(7L, "Owner", "owner@example.com");
        WorkspaceMember membership = member(workspace, owner, WorkspaceRole.OWNER);
        when(workspaceRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findById(new WorkspaceMemberId(21L, 7L)))
                .thenReturn(Optional.of(membership));
        when(memberRepository.countByWorkspace_IdAndRole(21L, WorkspaceRole.OWNER))
                .thenReturn(1L);

        assertThatThrownBy(() -> workspaceService.removeMember(21L, 7L))
                .isInstanceOf(WorkspaceMustHaveOwnerException.class);

        verify(memberRepository, never()).delete(any());
    }

    private static Workspace workspace(Long id, String name) {
        Workspace workspace = new Workspace(name);
        initialize(workspace, id);
        return workspace;
    }

    private static void initialize(Workspace workspace, Long id) {
        ReflectionTestUtils.setField(workspace, "id", id);
        ReflectionTestUtils.setField(workspace, "createdAt", TIMESTAMP);
        ReflectionTestUtils.setField(workspace, "updatedAt", TIMESTAMP);
    }

    private static User user(Long id, String name, String email) {
        User user = new User(name, email, "encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", TIMESTAMP);
        ReflectionTestUtils.setField(user, "updatedAt", TIMESTAMP);
        return user;
    }

    private static WorkspaceMember member(
            Workspace workspace,
            User user,
            WorkspaceRole role) {
        WorkspaceMember member = new WorkspaceMember(workspace, user, role);
        ReflectionTestUtils.setField(member, "joinedAt", TIMESTAMP);
        return member;
    }
}
