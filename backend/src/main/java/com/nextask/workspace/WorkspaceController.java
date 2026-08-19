package com.nextask.workspace;

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
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.api.UpdateWorkspaceMemberRequest;
import com.nextask.workspace.api.UpdateWorkspaceRequest;
import com.nextask.workspace.api.UserWorkspaceResponse;
import com.nextask.workspace.api.WorkspaceMemberResponse;
import com.nextask.workspace.api.WorkspaceResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.create(request);
        return ResponseEntity
                .created(URI.create("/api/workspaces/" + workspace.id()))
                .body(workspace);
    }

    @GetMapping
    PageResponse<UserWorkspaceResponse> findAllForUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workspaceService.findAllForUser(userId, page, size);
    }

    @GetMapping("/{workspaceId}")
    WorkspaceResponse findById(@PathVariable Long workspaceId) {
        return workspaceService.findById(workspaceId);
    }

    @PutMapping("/{workspaceId}")
    WorkspaceResponse update(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    ResponseEntity<Void> delete(@PathVariable Long workspaceId) {
        workspaceService.delete(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    PageResponse<WorkspaceMemberResponse> findMembers(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workspaceService.findMembers(workspaceId, page, size);
    }

    @PostMapping("/{workspaceId}/members")
    ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request) {
        WorkspaceMemberResponse member = workspaceService.addMember(workspaceId, request);
        return ResponseEntity
                .created(URI.create("/api/workspaces/" + workspaceId
                        + "/members/" + member.userId()))
                .body(member);
    }

    @PutMapping("/{workspaceId}/members/{userId}")
    WorkspaceMemberResponse updateMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceMemberRequest request) {
        return workspaceService.updateMember(workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    ResponseEntity<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        workspaceService.removeMember(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
