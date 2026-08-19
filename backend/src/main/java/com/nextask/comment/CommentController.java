package com.nextask.comment;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nextask.api.PageResponse;
import com.nextask.comment.api.CommentResponse;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.comment.api.UpdateCommentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/tasks/{taskId}/comments")
    ResponseEntity<CommentResponse> create(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse comment = commentService.create(taskId, request);
        return ResponseEntity
                .created(URI.create("/api/comments/" + comment.id()))
                .body(comment);
    }

    @GetMapping("/tasks/{taskId}/comments")
    PageResponse<CommentResponse> findAll(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return commentService.findAll(taskId, page, size);
    }

    @PutMapping("/comments/{commentId}")
    CommentResponse update(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
