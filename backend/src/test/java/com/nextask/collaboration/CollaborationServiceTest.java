package com.nextask.collaboration;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.nextask.comment.CommentRepository;
import com.nextask.comment.CommentService;
import com.nextask.comment.api.CommentResponse;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.comment.entity.Comment;
import com.nextask.entity.User;
import com.nextask.project.ProjectRepository;
import com.nextask.project.ProjectService;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.project.api.ProjectResponse;
import com.nextask.project.entity.Project;
import com.nextask.task.TaskRepository;
import com.nextask.task.TaskService;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.TaskResponse;
import com.nextask.task.api.UpdateTaskRequest;
import com.nextask.task.entity.Task;
import com.nextask.task.entity.TaskPriority;
import com.nextask.task.entity.TaskStatus;
import com.nextask.task.error.TaskConcurrentUpdateException;
import com.nextask.user.UserRepository;
import com.nextask.workspace.WorkspaceMembershipValidator;
import com.nextask.workspace.WorkspaceRepository;
import com.nextask.workspace.entity.Workspace;
import com.nextask.workspace.error.UserMustBelongToWorkspaceException;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceMembershipValidator membershipValidator;

    private ProjectService projectService;
    private TaskService taskService;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                workspaceRepository,
                userRepository,
                membershipValidator);
        taskService = new TaskService(
                taskRepository,
                projectRepository,
                userRepository,
                membershipValidator);
        commentService = new CommentService(
                commentRepository,
                taskRepository,
                userRepository,
                membershipValidator);
    }

    @Test
    void createsProjectOnlyForWorkspaceMember() {
        Workspace workspace = workspace(10L);
        User creator = user(1L, "Ada");
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            initialize(project, 20L);
            return project;
        });

        ProjectResponse response = projectService.create(
                10L,
                new CreateProjectRequest(" Product ", " ", 1L));

        verify(membershipValidator).requireMember(10L, 1L);
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.name()).isEqualTo("Product");
        assertThat(response.description()).isNull();
    }

    @Test
    void createsTaskInTodoWithWorkspaceMembers() {
        Workspace workspace = workspace(10L);
        User creator = user(1L, "Ada");
        User assignee = user(2L, "Grace");
        Project project = project(20L, workspace, creator);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            initialize(task, 30L);
            return task;
        });

        TaskResponse response = taskService.create(
                20L,
                new CreateTaskRequest(
                        " API contract ",
                        null,
                        TaskPriority.HIGH,
                        3,
                        2L,
                        1L,
                        null));

        verify(membershipValidator).requireMember(10L, 1L);
        verify(membershipValidator).requireMember(10L, 2L);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.assigneeId()).isEqualTo(2L);
        assertThat(response.position()).isEqualTo(3);
    }

    @Test
    void rejectsTaskAssigneeOutsideWorkspace() {
        Workspace workspace = workspace(10L);
        User creator = user(1L, "Ada");
        User outsider = user(2L, "Outsider");
        Project project = project(20L, workspace, creator);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(outsider));
        org.mockito.Mockito.doNothing()
                .when(membershipValidator).requireMember(10L, 1L);
        org.mockito.Mockito.doThrow(new UserMustBelongToWorkspaceException(10L, 2L))
                .when(membershipValidator).requireMember(10L, 2L);

        assertThatThrownBy(() -> taskService.create(
                20L,
                new CreateTaskRequest(
                        "API contract",
                        null,
                        TaskPriority.MEDIUM,
                        0,
                        2L,
                        1L,
                        null)))
                .isInstanceOf(UserMustBelongToWorkspaceException.class);

        verify(taskRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsCommentOnlyForWorkspaceMember() {
        Workspace workspace = workspace(10L);
        User creator = user(1L, "Ada");
        User author = user(2L, "Grace");
        Project project = project(20L, workspace, creator);
        Task task = task(30L, project, creator);
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            initialize(comment, 40L);
            return comment;
        });

        CommentResponse response = commentService.create(
                30L,
                new CreateCommentRequest(" Looks good ", 2L));

        verify(membershipValidator).requireMember(10L, 2L);
        assertThat(response.content()).isEqualTo("Looks good");
        assertThat(response.authorName()).isEqualTo("Grace");
    }

    @Test
    void reportsConcurrentTaskUpdate() {
        Workspace workspace = workspace(10L);
        User creator = user(1L, "Ada");
        Project project = project(20L, workspace, creator);
        Task task = task(30L, project, creator);
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task))
                .thenThrow(new OptimisticLockingFailureException("stale version"));

        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated",
                null,
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                0,
                null,
                null);

        assertThatThrownBy(() -> taskService.update(30L, request))
                .isInstanceOf(TaskConcurrentUpdateException.class);
    }

    private static Workspace workspace(Long id) {
        Workspace workspace = new Workspace("Workspace");
        ReflectionTestUtils.setField(workspace, "id", id);
        ReflectionTestUtils.setField(workspace, "createdAt", TIMESTAMP);
        ReflectionTestUtils.setField(workspace, "updatedAt", TIMESTAMP);
        return workspace;
    }

    private static User user(Long id, String name) {
        User user = new User(name, name.toLowerCase() + "@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", TIMESTAMP);
        ReflectionTestUtils.setField(user, "updatedAt", TIMESTAMP);
        return user;
    }

    private static Project project(Long id, Workspace workspace, User creator) {
        Project project = new Project(workspace, "Project", null, creator);
        initialize(project, id);
        return project;
    }

    private static Task task(Long id, Project project, User creator) {
        Task task = new Task(
                project,
                "Task",
                null,
                TaskPriority.MEDIUM,
                0,
                null,
                creator,
                null);
        initialize(task, id);
        return task;
    }

    private static void initialize(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", TIMESTAMP);
        ReflectionTestUtils.setField(entity, "updatedAt", TIMESTAMP);
    }
}
