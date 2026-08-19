package com.nextask.task.api;

import java.time.OffsetDateTime;

import com.nextask.task.entity.TaskPriority;
import com.nextask.task.entity.TaskStatus;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        int position,
        Long assigneeId,
        Long createdById,
        OffsetDateTime dueAt,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
