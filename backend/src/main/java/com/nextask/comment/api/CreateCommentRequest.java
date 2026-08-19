package com.nextask.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must have at most 2000 characters")
        String content,

        @NotNull(message = "authorId is required")
        @Positive(message = "authorId must be positive")
        Long authorId) {
}
