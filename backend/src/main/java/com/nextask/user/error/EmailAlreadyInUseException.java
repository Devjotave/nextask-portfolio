package com.nextask.user.error;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("A user with this email already exists");
    }
}
