package com.nextask.task.error;

public class TaskConcurrentUpdateException extends RuntimeException {

    public TaskConcurrentUpdateException(Long taskId) {
        super("Task " + taskId + " was changed by another request; reload and try again");
    }
}
