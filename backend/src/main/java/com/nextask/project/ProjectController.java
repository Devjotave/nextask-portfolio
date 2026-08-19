package com.nextask.project;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nextask.api.PageResponse;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.project.api.ProjectResponse;
import com.nextask.project.api.UpdateProjectRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    ResponseEntity<ProjectResponse> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = projectService.create(workspaceId, request);
        return ResponseEntity
                .created(URI.create("/api/projects/" + project.id()))
                .body(project);
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    PageResponse<ProjectResponse> findAll(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return projectService.findAll(workspaceId, page, size);
    }

    @GetMapping("/projects/{projectId}")
    ProjectResponse findById(@PathVariable Long projectId) {
        return projectService.findById(projectId);
    }

    @PutMapping("/projects/{projectId}")
    ProjectResponse update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectId, request);
    }

    @DeleteMapping("/projects/{projectId}")
    ResponseEntity<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}
