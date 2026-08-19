package com.nextask.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.api.PageResponse;
import com.nextask.auth.AuthenticatedUser;
import com.nextask.comment.api.CommentResponse;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.comment.api.UpdateCommentRequest;
import com.nextask.comment.entity.Comment;
import com.nextask.comment.error.CommentNotFoundException;
import com.nextask.entity.User;
import com.nextask.task.TaskRepository;
import com.nextask.task.entity.Task;
import com.nextask.task.error.TaskNotFoundException;
import com.nextask.user.UserRepository;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.workspace.WorkspaceMembershipValidator;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipValidator membershipValidator;
    private final AuthenticatedUser authenticatedUser;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            WorkspaceMembershipValidator membershipValidator,
            AuthenticatedUser authenticatedUser) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.membershipValidator = membershipValidator;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional
    public CommentResponse create(Long taskId, CreateCommentRequest request) {
        Task task = findTask(taskId);
        Long workspaceId = task.getProject().getWorkspace().getId();
        Long userId = authenticatedUser.id();
        membershipValidator.requireAccess(workspaceId, userId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Comment comment = new Comment(task, author, request.content());
        return toResponse(commentRepository.saveAndFlush(comment));
    }

    public PageResponse<CommentResponse> findAll(Long taskId, int page, int size) {
        Task task = findTask(taskId);
        membershipValidator.requireAccess(
                task.getProject().getWorkspace().getId(),
                authenticatedUser.id());
        Page<CommentResponse> comments = commentRepository.findByTask_Id(
                        taskId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(CommentService::toResponse);
        return PageResponse.from(comments);
    }

    @Transactional
    public CommentResponse update(Long commentId, UpdateCommentRequest request) {
        Comment comment = findComment(commentId);
        requireAuthorOrOwner(comment);
        comment.edit(request.content());
        return toResponse(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public void delete(Long commentId) {
        Comment comment = findComment(commentId);
        requireAuthorOrOwner(comment);
        commentRepository.delete(comment);
        commentRepository.flush();
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void requireAuthorOrOwner(Comment comment) {
        Long workspaceId = comment.getTask().getProject().getWorkspace().getId();
        membershipValidator.requireSelfOrOwner(
                workspaceId,
                authenticatedUser.id(),
                comment.getAuthor().getId());
    }

    private static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
