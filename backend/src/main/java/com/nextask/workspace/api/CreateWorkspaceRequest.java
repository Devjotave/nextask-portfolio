package com.nextask.workspace.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must have at most 120 characters")
        String name,

        @NotNull(message = "ownerId is required")
        @Positive(message = "ownerId must be positive")
        Long ownerId) {
}
