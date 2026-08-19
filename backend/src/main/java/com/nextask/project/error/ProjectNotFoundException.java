package com.nextask.project.error;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long projectId) {
        super("Project " + projectId + " was not found");
    }
}
