package com.nextask.workspace.error;

public class WorkspaceAccessDeniedException extends RuntimeException {

    public WorkspaceAccessDeniedException(Long workspaceId) {
        super("Access to workspace " + workspaceId + " is not allowed");
    }
}
