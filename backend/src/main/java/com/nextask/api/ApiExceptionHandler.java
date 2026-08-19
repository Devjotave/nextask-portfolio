package com.nextask.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nextask.user.error.EmailAlreadyInUseException;
import com.nextask.user.error.UserHasWorkspaceMembershipsException;
import com.nextask.user.error.UserNotFoundException;
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
    private static final URI VALIDATION_FAILED = URI.create("urn:nextask:problem:validation-failed");

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
