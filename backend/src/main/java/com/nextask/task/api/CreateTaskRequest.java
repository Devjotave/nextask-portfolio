package com.nextask.task.api;

import java.time.OffsetDateTime;

import com.nextask.task.entity.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must have at most 200 characters")
        String title,

        @Size(max = 4000, message = "description must have at most 4000 characters")
        String description,

        @NotNull(message = "priority is required")
        TaskPriority priority,

        @NotNull(message = "position is required")
        @PositiveOrZero(message = "position must not be negative")
        Integer position,

        @Positive(message = "assigneeId must be positive")
        Long assigneeId,

        @NotNull(message = "createdById is required")
        @Positive(message = "createdById must be positive")
        Long createdById,

        OffsetDateTime dueAt) {
}
