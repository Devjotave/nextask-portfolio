package com.nextask.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nextask.comment.error.CommentNotFoundException;
import com.nextask.project.error.ProjectNotFoundException;
import com.nextask.task.error.TaskConcurrentUpdateException;
import com.nextask.task.error.TaskNotFoundException;
import com.nextask.user.error.EmailAlreadyInUseException;
import com.nextask.user.error.UserHasWorkspaceMembershipsException;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.workspace.error.UserMustBelongToWorkspaceException;
import com.nextask.workspace.error.WorkspaceAccessDeniedException;
import com.nextask.workspace.error.WorkspaceMemberAlreadyExistsException;
import com.nextask.workspace.error.WorkspaceMemberNotFoundException;
import com.nextask.workspace.error.WorkspaceMustHaveOwnerException;
import com.nextask.workspace.error.WorkspaceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI USER_NOT_FOUND = URI.create("urn:nextask:problem:user-not-found");
    private static final URI EMAIL_ALREADY_IN_USE = URI.create("urn:nextask:problem:email-already-in-use");
    private static final URI USER_HAS_MEMBERSHIPS = URI.create("urn:nextask:problem:user-has-workspace-memberships");
    private static final URI WORKSPACE_NOT_FOUND = URI.create("urn:nextask:problem:workspace-not-found");
    private static final URI WORKSPACE_MEMBER_NOT_FOUND = URI.create("urn:nextask:problem:workspace-member-not-found");
    private static final URI WORKSPACE_MEMBER_ALREADY_EXISTS = URI.create("urn:nextask:problem:workspace-member-already-exists");
    private static final URI WORKSPACE_MUST_HAVE_OWNER = URI.create("urn:nextask:problem:workspace-must-have-owner");
    private static final URI USER_MUST_BELONG_TO_WORKSPACE = URI.create("urn:nextask:problem:user-must-belong-to-workspace");
    private static final URI PROJECT_NOT_FOUND = URI.create("urn:nextask:problem:project-not-found");
    private static final URI TASK_NOT_FOUND = URI.create("urn:nextask:problem:task-not-found");
    private static final URI COMMENT_NOT_FOUND = URI.create("urn:nextask:problem:comment-not-found");
    private static final URI TASK_CONCURRENT_UPDATE = URI.create("urn:nextask:problem:task-concurrent-update");
    private static final URI AUTHENTICATION_FAILED = URI.create("urn:nextask:problem:authentication-failed");
    private static final URI WORKSPACE_ACCESS_DENIED = URI.create("urn:nextask:problem:workspace-access-denied");
    private static final URI VALIDATION_FAILED = URI.create("urn:nextask:problem:validation-failed");

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Email or password is incorrect");
        problem.setType(AUTHENTICATION_FAILED);
        problem.setTitle("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, USER_NOT_FOUND, "User not found", exception);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    ResponseEntity<ProblemDetail> handleEmailAlreadyInUse(EmailAlreadyInUseException exception) {
        return problem(HttpStatus.CONFLICT, EMAIL_ALREADY_IN_USE, "Email already in use", exception);
    }

    @ExceptionHandler(UserHasWorkspaceMembershipsException.class)
    ResponseEntity<ProblemDetail> handleUserHasMemberships(
            UserHasWorkspaceMembershipsException exception) {
        return problem(
                HttpStatus.CONFLICT,
                USER_HAS_MEMBERSHIPS,
                "User still belongs to workspaces",
                exception);
    }

    @ExceptionHandler(WorkspaceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleWorkspaceNotFound(WorkspaceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND, "Workspace not found", exception);
    }

    @ExceptionHandler(WorkspaceMemberNotFoundException.class)
    ResponseEntity<ProblemDetail> handleWorkspaceMemberNotFound(
            WorkspaceMemberNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                WORKSPACE_MEMBER_NOT_FOUND,
                "Workspace member not found",
                exception);
    }

    @ExceptionHandler(WorkspaceMemberAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> handleWorkspaceMemberAlreadyExists(
            WorkspaceMemberAlreadyExistsException exception) {
        return problem(
                HttpStatus.CONFLICT,
                WORKSPACE_MEMBER_ALREADY_EXISTS,
                "Workspace member already exists",
                exception);
    }

    @ExceptionHandler(WorkspaceMustHaveOwnerException.class)
    ResponseEntity<ProblemDetail> handleWorkspaceMustHaveOwner(
            WorkspaceMustHaveOwnerException exception) {
        return problem(
                HttpStatus.CONFLICT,
                WORKSPACE_MUST_HAVE_OWNER,
                "Workspace must have an owner",
                exception);
    }

    @ExceptionHandler(UserMustBelongToWorkspaceException.class)
    ResponseEntity<ProblemDetail> handleUserMustBelongToWorkspace(
            UserMustBelongToWorkspaceException exception) {
        return problem(
                HttpStatus.CONFLICT,
                USER_MUST_BELONG_TO_WORKSPACE,
                "User must belong to workspace",
                exception);
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleWorkspaceAccessDenied(
            WorkspaceAccessDeniedException exception) {
        return problem(
                HttpStatus.FORBIDDEN,
                WORKSPACE_ACCESS_DENIED,
                "Workspace access denied",
                exception);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    ResponseEntity<ProblemDetail> handleProjectNotFound(ProjectNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND, "Project not found", exception);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ProblemDetail> handleTaskNotFound(TaskNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, TASK_NOT_FOUND, "Task not found", exception);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    ResponseEntity<ProblemDetail> handleCommentNotFound(CommentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND, "Comment not found", exception);
    }

    @ExceptionHandler(TaskConcurrentUpdateException.class)
    ResponseEntity<ProblemDetail> handleTaskConcurrentUpdate(
            TaskConcurrentUpdateException exception) {
        return problem(
                HttpStatus.CONFLICT,
                TASK_CONCURRENT_UPDATE,
                "Task was updated concurrently",
                exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid");
        problem.setType(VALIDATION_FAILED);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            URI type,
            String title,
            RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(type);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
