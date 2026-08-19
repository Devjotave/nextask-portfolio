package com.nextask.user.error;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User " + id + " was not found");
    }
}
