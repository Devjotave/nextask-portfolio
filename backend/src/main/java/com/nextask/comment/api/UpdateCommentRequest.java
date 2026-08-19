package com.nextask.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must have at most 2000 characters")
        String content) {
}
