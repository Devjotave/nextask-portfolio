package com.nextask.comment.error;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long commentId) {
        super("Comment " + commentId + " was not found");
    }
}
