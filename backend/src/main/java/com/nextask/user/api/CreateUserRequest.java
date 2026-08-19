package com.nextask.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must have at most 120 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 254, message = "email must have at most 254 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 128, message = "password must have between 8 and 128 characters")
        String password) {
}
