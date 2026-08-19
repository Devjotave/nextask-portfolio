package com.nextask.task.error;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("Task " + taskId + " was not found");
    }
}
