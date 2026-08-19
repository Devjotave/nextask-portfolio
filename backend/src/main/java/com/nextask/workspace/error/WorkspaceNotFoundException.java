package com.nextask.workspace.error;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(Long workspaceId) {
        super("Workspace " + workspaceId + " was not found");
    }
}
