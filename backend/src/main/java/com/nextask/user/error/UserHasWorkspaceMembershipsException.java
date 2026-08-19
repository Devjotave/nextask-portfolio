package com.nextask.user.error;

public class UserHasWorkspaceMembershipsException extends RuntimeException {

    public UserHasWorkspaceMembershipsException(Long userId) {
        super("User " + userId + " still belongs to one or more workspaces");
    }
}
