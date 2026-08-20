package com.nextask.workspace;

import static com.nextask.openapi.OpenApiConfiguration.BEARER_AUTH;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/workspaces")
@Tag(name = "Workspaces", description = "Workspaces, membership and OWNER administration")
@SecurityRequirement(name = BEARER_AUTH)
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @Operation(summary = "Create a workspace with the authenticated user as OWNER")
    @ApiResponse(responseCode = "201", description = "Workspace created")
    ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.create(request);
        return ResponseEntity
                .created(URI.create("/api/workspaces/" + workspace.id()))
                .body(workspace);
    }

    @GetMapping
    @Operation(summary = "List workspaces of the authenticated user")
    PageResponse<UserWorkspaceResponse> findAllForCurrentUser(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workspaceService.findAllForCurrentUser(page, size);
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get a workspace")
    WorkspaceResponse findById(@PathVariable Long workspaceId) {
        return workspaceService.findById(workspaceId);
    }

    @PutMapping("/{workspaceId}")
    @Operation(summary = "Rename a workspace", description = "Requires the OWNER role.")
    WorkspaceResponse update(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "Delete a workspace", description = "Requires the OWNER role.")
    @ApiResponse(responseCode = "204", description = "Workspace deleted")
    ResponseEntity<Void> delete(@PathVariable Long workspaceId) {
        workspaceService.delete(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "List workspace members")
    PageResponse<WorkspaceMemberResponse> findMembers(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workspaceService.findMembers(workspaceId, page, size);
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "Add a workspace member", description = "Requires the OWNER role.")
    @ApiResponse(responseCode = "201", description = "Member added")
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
    @Operation(summary = "Change a workspace member role", description = "Requires the OWNER role.")
    WorkspaceMemberResponse updateMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceMemberRequest request) {
        return workspaceService.updateMember(workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @Operation(summary = "Remove a workspace member", description = "Requires the OWNER role.")
    @ApiResponse(responseCode = "204", description = "Member removed")
    ResponseEntity<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        workspaceService.removeMember(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
