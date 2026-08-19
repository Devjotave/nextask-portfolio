package com.nextask.comment.api;

import java.time.OffsetDateTime;

public record CommentResponse(
        Long id,
        Long taskId,
        Long authorId,
        String authorName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
