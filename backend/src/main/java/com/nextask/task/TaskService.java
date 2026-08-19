package com.nextask.task;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.api.PageResponse;
import com.nextask.entity.User;
import com.nextask.project.ProjectRepository;
import com.nextask.project.entity.Project;
import com.nextask.project.error.ProjectNotFoundException;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.TaskResponse;
import com.nextask.task.api.UpdateTaskRequest;
import com.nextask.task.entity.Task;
import com.nextask.task.entity.TaskStatus;
import com.nextask.task.error.TaskConcurrentUpdateException;
import com.nextask.task.error.TaskNotFoundException;
import com.nextask.user.UserRepository;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.workspace.WorkspaceMembershipValidator;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipValidator membershipValidator;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            WorkspaceMembershipValidator membershipValidator) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.membershipValidator = membershipValidator;
    }

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request) {
        Project project = findProject(projectId);
        Long workspaceId = project.getWorkspace().getId();
        User creator = findMember(workspaceId, request.createdById());
        User assignee = request.assigneeId() == null
                ? null
                : findMember(workspaceId, request.assigneeId());

        Task task = new Task(
                project,
                request.title(),
                request.description(),
                request.priority(),
                request.position(),
                assignee,
                creator,
                request.dueAt());
        return toResponse(taskRepository.saveAndFlush(task));
    }

    public PageResponse<TaskResponse> findAll(
            Long projectId,
            TaskStatus status,
            int page,
            int size) {
        findProject(projectId);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "position")
                        .and(Sort.by(Sort.Direction.ASC, "id")));
        Page<Task> tasks = status == null
                ? taskRepository.findByProject_Id(projectId, pageable)
                : taskRepository.findByProject_IdAndStatus(projectId, status, pageable);
        return PageResponse.from(tasks.map(TaskService::toResponse));
    }

    public TaskResponse findById(Long taskId) {
        return toResponse(findTask(taskId));
    }

    @Transactional
    public TaskResponse update(Long taskId, UpdateTaskRequest request) {
        Task task = findTask(taskId);
        Long workspaceId = task.getProject().getWorkspace().getId();
        User assignee = request.assigneeId() == null
                ? null
                : findMember(workspaceId, request.assigneeId());
        task.edit(
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.position(),
                assignee,
                request.dueAt());

        try {
            return toResponse(taskRepository.saveAndFlush(task));
        } catch (OptimisticLockingFailureException exception) {
            throw new TaskConcurrentUpdateException(taskId);
        }
    }

    @Transactional
    public void delete(Long taskId) {
        Task task = findTask(taskId);
        taskRepository.delete(task);
        taskRepository.flush();
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private User findMember(Long workspaceId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        membershipValidator.requireMember(workspaceId, userId);
        return user;
    }

    private static TaskResponse toResponse(Task task) {
        User assignee = task.getAssignee();
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getPosition(),
                assignee == null ? null : assignee.getId(),
                task.getCreatedBy().getId(),
                task.getDueAt(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
