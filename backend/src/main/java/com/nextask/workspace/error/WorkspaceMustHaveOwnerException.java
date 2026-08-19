package com.nextask.workspace.error;

public class WorkspaceMustHaveOwnerException extends RuntimeException {

    public WorkspaceMustHaveOwnerException(Long workspaceId) {
        super("Workspace " + workspaceId + " must keep at least one owner");
    }
}
