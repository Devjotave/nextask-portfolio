package com.nextask.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nextask.api.PageResponse;
import com.nextask.auth.AuthenticatedUser;
import com.nextask.entity.User;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.project.api.ProjectResponse;
import com.nextask.project.api.UpdateProjectRequest;
import com.nextask.project.entity.Project;
import com.nextask.project.error.ProjectNotFoundException;
import com.nextask.user.UserRepository;
import com.nextask.user.error.UserNotFoundException;
import com.nextask.workspace.WorkspaceMembershipValidator;
import com.nextask.workspace.WorkspaceRepository;
import com.nextask.workspace.entity.Workspace;
import com.nextask.workspace.error.WorkspaceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipValidator membershipValidator;
    private final AuthenticatedUser authenticatedUser;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceMembershipValidator membershipValidator,
            AuthenticatedUser authenticatedUser) {
        this.projectRepository = projectRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipValidator = membershipValidator;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional
    public ProjectResponse create(Long workspaceId, CreateProjectRequest request) {
        Workspace workspace = findWorkspace(workspaceId);
        Long userId = authenticatedUser.id();
        membershipValidator.requireAccess(workspaceId, userId);
        User creator = findUser(userId);

        Project project = new Project(
                workspace,
                request.name(),
                request.description(),
                creator);
        return toResponse(projectRepository.saveAndFlush(project));
    }

    public PageResponse<ProjectResponse> findAll(Long workspaceId, int page, int size) {
        findWorkspace(workspaceId);
        membershipValidator.requireAccess(workspaceId, authenticatedUser.id());
        Page<ProjectResponse> projects = projectRepository.findByWorkspace_Id(
                        workspaceId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(ProjectService::toResponse);
        return PageResponse.from(projects);
    }

    public ProjectResponse findById(Long projectId) {
        return toResponse(findProjectWithAccess(projectId));
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request) {
        Project project = findProjectWithAccess(projectId);
        project.edit(request.name(), request.description());
        return toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public void delete(Long projectId) {
        Project project = findProjectWithAccess(projectId);
        projectRepository.delete(project);
        projectRepository.flush();
    }

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Project findProjectWithAccess(Long projectId) {
        Project project = findProject(projectId);
        membershipValidator.requireAccess(
                project.getWorkspace().getId(),
                authenticatedUser.id());
        return project;
    }

    private static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedBy().getId(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
