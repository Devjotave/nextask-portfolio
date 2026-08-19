package com.nextask.workspace.error;

public class WorkspaceMemberAlreadyExistsException extends RuntimeException {

    public WorkspaceMemberAlreadyExistsException(Long workspaceId, Long userId) {
        super("User " + userId + " already belongs to workspace " + workspaceId);
    }
}
