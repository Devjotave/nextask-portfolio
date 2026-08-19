package com.nextask.workspace.error;

public class UserMustBelongToWorkspaceException extends RuntimeException {

    public UserMustBelongToWorkspaceException(Long workspaceId, Long userId) {
        super("User " + userId + " must belong to workspace " + workspaceId);
    }
}
