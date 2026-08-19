package com.nextask.project.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must have at most 120 characters")
        String name,

        @Size(max = 1000, message = "description must have at most 1000 characters")
        String description,

        @NotNull(message = "createdById is required")
        @Positive(message = "createdById must be positive")
        Long createdById) {
}
